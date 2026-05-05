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
      String fileKey = getFileKey(fileInfo);
      Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> uploadSink =
          createUploadSink(getBucketType(), fileKey);

      String fileName = fileInfo.fileName();
      if (fileName == null || fileName.isBlank()) {
        throw new FileUploadTypeException("Uploaded file has no filename.");
      }

      AtomicReference<String> detectedMimeTypeRef = new AtomicReference<>(null);
      Flow<ByteString, ByteString, ?> sniffingFlow =
          fileTypeValidation.sniffingFlow(fileName, detectedMimeTypeRef, getAllowedFileTypes());

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
   * Returns the list of allowed file types for validation. Subclasses may override to restrict
   * uploads to a specific set of types.
   */
  protected ImmutableList<String> getAllowedFileTypes() {
    return fileTypeValidation.getAllowedFileTypes();
  }
}
