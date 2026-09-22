package parsers;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.cloud.MultipartUploadSinks;
import play.core.parsers.Multipart;
import play.http.DefaultHttpErrorHandler;
import play.http.HttpErrorHandler;
import play.libs.F;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
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
 *
 * <p>Play runs the body parser before any action composition, so annotations like {@code @Secure}
 * on the controller method only run after the file has already been streamed to storage. To close
 * that gap, {@link #apply} calls {@link #isAuthorized} before reading any bytes and rejects the
 * request with a 403 if the caller is not allowed to upload. Subclasses must implement the same
 * rule the action enforces so that anything the parser lets through also passes the action.
 */
public abstract class StreamingMultipartBodyParser
    extends BodyParser.DelegatingMultipartFormDataBodyParser<String> {
  private static final int CHUNK_SIZE = 1024 * 1024; // 1 MiB
  private static final String FORBIDDEN_MESSAGE = "Not authorized to upload files.";

  private final Materializer materializer;
  private final HttpErrorHandler errorHandler;
  private final MultipartUploadSinks uploadSinks;
  private final FileTypeValidation fileTypeValidation;

  public StreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      MultipartUploadSinks streamingMultipartUploadSinks,
      FileTypeValidation fileTypeValidation,
      long maxFileSize) {
    super(materializer, CHUNK_SIZE, maxFileSize, /* allowEmptyFiles= */ false, errorHandler);
    this.materializer = materializer;
    this.errorHandler = errorHandler;
    this.uploadSinks = streamingMultipartUploadSinks;
    this.fileTypeValidation = fileTypeValidation;
  }

  /**
   * Parses the request path and runs the authorization check before any of the body is consumed. An
   * unauthorized caller gets a 403 and the upstream is cancelled, so no upload sink is ever created
   * for them.
   */
  @Override
  public final Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<String>>> apply(
      Http.RequestHeader request) {
    parseRequestPath(request);

    CompletionStage<Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<String>>>>
        gated =
            isAuthorized(request)
                .thenApply(authorized -> authorized ? super.apply(request) : forbidden(request));

    return Accumulator.flatten(gated, materializer);
  }

  private Accumulator<ByteString, F.Either<Result, Http.MultipartFormData<String>>> forbidden(
      Http.RequestHeader request) {
    CompletionStage<F.Either<Result, Http.MultipartFormData<String>>> rejected =
        errorHandler
            .onClientError(request, Http.Status.FORBIDDEN, FORBIDDEN_MESSAGE)
            .thenApply(result -> F.Either.<Result, Http.MultipartFormData<String>>Left(result));

    return Accumulator.<ByteString, F.Either<Result, Http.MultipartFormData<String>>>done(rejected);
  }

  /**
   * Reads any ids the subclass needs out of the request path. Runs before {@link #isAuthorized} so
   * the authorization check can use them. The default does nothing.
   */
  protected void parseRequestPath(Http.RequestHeader request) {}

  /**
   * Decides whether the caller may upload. This runs before the body is read and must mirror the
   * checks the controller action performs, since it is the only check that happens before the file
   * lands in storage.
   */
  protected abstract CompletionStage<Boolean> isAuthorized(Http.RequestHeader request);

  @Override
  public Function<Multipart.FileInfo, Accumulator<ByteString, FilePart<String>>>
      createFilePartHandler() {
    return fileInfo -> {
      String fileKey = getFileKey(fileInfo);
      Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> uploadSink =
          createUploadSink(getBucketType(), fileKey);

      String fileName = fileInfo.fileName();
      if (fileName == null || fileName.isBlank()) {
        throw new FileUploadTypeException("Uploaded file has no filename.");
      }

      AtomicReference<String> detectedMimeTypeRef = new AtomicReference<>(null);
      ImmutableList<FileTypeSpecifier> allowed = getAllowedFileTypeSpecifiers();
      if (allowed.isEmpty()) {
        throw new IllegalArgumentException("At least one FileTypeSpecifier is required");
      }

      Flow<ByteString, ByteString, ?> sniffingFlow =
          fileTypeValidation.sniffingFlow(fileName, detectedMimeTypeRef, allowed);

      // Map upload sink to an output value, prepending the sniffing flow
      Sink<ByteString, CompletionStage<FilePart<String>>> mappedSink =
          sniffingFlow
              .toMat(uploadSink, Keep.right())
              .mapMaterializedValue(
                  completionStage -> {
                    // Map the completion stage to a FilePart with the appropriate metadata
                    return completionStage.thenApply(
                        uploadResult -> {
                          // Here we can construct a FilePart with the metadata and the result of
                          // the
                          // upload
                          return new FilePart<String>(
                              fileInfo.partName(), fileName, detectedMimeTypeRef.get(), fileKey);
                        });
                  });

      // Create an accumulator that streams the file data to the mapped upload sink
      return Accumulator.fromSink(mappedSink);
    };
  }

  // Chooses between Pekko connector sinks based on the configured storage provider.
  // https://pekko.apache.org/docs/pekko-connectors/1.2/index.html
  protected Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> createUploadSink(
      BucketType bucketType, String fileKey) {
    return uploadSinks.getSinkForCloudProvider(bucketType, fileKey, CHUNK_SIZE);
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
