package parsers;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.connectors.s3.FailedUpload;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parsers.cloud.MultipartUploadSinks;
import play.core.parsers.Multipart;
import play.http.DefaultHttpErrorHandler;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Http.MultipartFormData.FilePart;
import services.cloud.BucketType;

/**
 * Abstract class for performing a streaming upload of multipart form data.
 *
 * <p>This class extends the Play Framework's {@code DelegatingMultipartFormDataBodyParser}, but
 * overrides the parsing logic to allow for streaming the multipart form data directly to a
 * destination (e.g., file system, cloud storage)
 *
 * <p>Subclasses provide the implementation for handling the streaming, e.g. to different cloud
 * storage providers or a local file system. Each {@link FilePart} produced by this parser carries
 * the cloud-storage file key as its ref so the action can read it back.
 */
public abstract class StreamingMultipartBodyParser
    extends BodyParser.DelegatingMultipartFormDataBodyParser<String> {
  private static final Logger logger = LoggerFactory.getLogger(StreamingMultipartBodyParser.class);
  private static final int CHUNK_SIZE = 1024 * 1024; // 1 MiB
  private final MultipartUploadSinks uploadSinks;
  private final FileTypeValidation fileTypeValidation;

  public StreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      MultipartUploadSinks streamingMultipartUploadSinks,
      FileTypeValidation fileTypeValidation,
      long maxFileSize) {
    super(materializer, CHUNK_SIZE, maxFileSize, /* allowEmptyFiles= */ false, errorHandler);
    this.uploadSinks = streamingMultipartUploadSinks;
    this.fileTypeValidation = fileTypeValidation;
  }

  @Override
  public Function<Multipart.FileInfo, Accumulator<ByteString, FilePart<String>>>
      createFilePartHandler() {
    return fileInfo -> {
      String fileName = fileInfo.fileName();
      if (fileName == null || fileName.isBlank()) {
        logger.warn("Rejecting streaming upload: uploaded file has no filename.");
        throw new FileUploadTypeException("Uploaded file has no filename.");
      }

      ImmutableList<FileTypeSpecifier> allowed = getAllowedFileTypeSpecifiers();
      if (allowed.isEmpty()) {
        throw new IllegalArgumentException("At least one FileTypeSpecifier is required");
      }

      String fileKey = getFileKey(fileInfo);
      Optional<String> contentType = FileTypeValidation.determineMimeType(fileName);

      Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> uploadSink =
          createUploadSink(getBucketType(), fileKey, contentType, getContentDisposition());

      AtomicReference<String> detectedMimeTypeRef = new AtomicReference<>(null);
      AtomicReference<FileUploadTypeException> validationErrorRef = new AtomicReference<>(null);
      Flow<ByteString, ByteString, ?> sniffingFlow =
          fileTypeValidation
              .sniffingFlow(fileName, detectedMimeTypeRef, allowed)
              .mapError(
                  FileUploadTypeException.class,
                  e -> {
                    validationErrorRef.set(e);
                    return e;
                  });

      // Map upload sink to an output value, prepending the sniffing flow
      Sink<ByteString, CompletionStage<FilePart<String>>> mappedSink =
          sniffingFlow
              .toMat(uploadSink, Keep.right())
              .mapMaterializedValue(
                  completionStage ->
                      completionStage.handle(
                          (uploadResult, throwable) -> {
                            if (throwable != null) {
                              throw handleUploadFailure(throwable, validationErrorRef.get());
                            }
                            if (uploadResult == null
                                || uploadResult.getStatus()
                                    != StreamingMultipartUploadResult.Status.SUCCESS) {
                              throw handleFailedUploadResult(
                                  uploadResult, validationErrorRef.get());
                            }
                            return new FilePart<String>(
                                fileInfo.partName(), fileName, detectedMimeTypeRef.get(), fileKey);
                          }));

      // Create an accumulator that streams the file data to the mapped upload sink
      return Accumulator.fromSink(mappedSink);
    };
  }

  private static RuntimeException handleUploadFailure(
      Throwable throwable, FileUploadTypeException capturedValidationError) {
    if (capturedValidationError != null) {
      logger.warn(
          "Streaming upload rejected by file type validation: {}",
          capturedValidationError.getMessage());
      return capturedValidationError;
    }

    Throwable rootCause = unwrapAsyncWrapper(throwable);
    Optional<FileUploadTypeException> typeException = findFileUploadTypeException(rootCause);
    if (typeException.isPresent()) {
      logger.warn(
          "Streaming upload rejected by file type validation: {}",
          typeException.get().getMessage());
      return typeException.get();
    }

    logger.error("Streaming upload failed", rootCause);
    if (rootCause instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    return new RuntimeException("Streaming upload failed", rootCause);
  }

  private static RuntimeException handleFailedUploadResult(
      StreamingMultipartUploadResult uploadResult,
      FileUploadTypeException capturedValidationError) {
    if (capturedValidationError != null) {
      logger.warn(
          "Streaming upload rejected by file type validation: {}",
          capturedValidationError.getMessage());
      return capturedValidationError;
    }

    if (uploadResult == null) {
      logger.error("Streaming upload failed: upload sink returned null result");
      return new RuntimeException("Streaming upload failed: missing upload result");
    }

    if (uploadResult.getCause().isPresent()) {
      return handleUploadFailure(
          uploadResult.getCause().get(), /* capturedValidationError= */ null);
    }

    String errorDetail =
        uploadResult.getErrorMessage().orElse("status=" + uploadResult.getStatus());
    logger.error(
        "Streaming upload failed (storage={}, status={}): {}",
        uploadResult.getStorageServiceName(),
        uploadResult.getStatus(),
        errorDetail);
    return new RuntimeException(
        String.format(
            "Streaming upload failed (%s, %s): %s",
            uploadResult.getStorageServiceName(), uploadResult.getStatus(), errorDetail));
  }

  private static Throwable unwrapAsyncWrapper(Throwable throwable) {
    Throwable current = throwable;
    while ((current instanceof CompletionException || current instanceof ExecutionException)
        && current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }

  private static Optional<FileUploadTypeException> findFileUploadTypeException(
      Throwable throwable) {
    Throwable current = throwable;
    for (int depth = 0; depth < 10 && current != null; depth++) {
      if (current instanceof FileUploadTypeException typeException) {
        return Optional.of(typeException);
      }
      if (current instanceof FailedUpload failedUpload) {
        for (Throwable reason : failedUpload.getReasons()) {
          Optional<FileUploadTypeException> nested = findFileUploadTypeException(reason);
          if (nested.isPresent()) {
            return nested;
          }
        }
      }
      current = current.getCause();
    }
    return Optional.empty();
  }

  // Chooses between Pekko connector sinks based on the configured storage provider.
  // https://pekko.apache.org/docs/pekko-connectors/1.2/index.html
  protected Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> createUploadSink(
      BucketType bucketType,
      String fileKey,
      Optional<String> contentType,
      Optional<String> contentDisposition) {
    return uploadSinks.getSinkForCloudProvider(
        bucketType, fileKey, contentType, contentDisposition, CHUNK_SIZE);
  }

  protected Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> createUploadSink(
      BucketType bucketType, String fileKey) {
    return uploadSinks.getSinkForCloudProvider(bucketType, fileKey, CHUNK_SIZE);
  }

  /**
   * Optional Content-Disposition header (e.g. "inline") to apply to the uploaded object in cloud
   * storage.
   */
  protected Optional<String> getContentDisposition() {
    return Optional.empty();
  }

  /** Returns the file path within cloud storage for this upload. */
  protected abstract String getFileKey(Multipart.FileInfo fileInfo);

  /** Returns the bucket type for streaming the file. */
  protected abstract BucketType getBucketType();

  /**
   * Allowed upload types for {@link FileTypeValidation#sniffingFlow}. The parser subclass defines
   * what it accepts (same tokens as {@code file_upload_allowed_file_type_specifiers} when parsed
   * with {@link FileTypeSpecifier#parseCommaSeparated}).
   */
  protected abstract ImmutableList<FileTypeSpecifier> getAllowedFileTypeSpecifiers();
}
