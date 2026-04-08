package parsers;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.cloud.MultipartUploadSinks;
import play.http.DefaultHttpErrorHandler;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Http.MultipartFormData.FilePart;

/**
 * Abstract class for performing a streaming upload of multipart form data.
 *
 * <p>This class extends the Play Framework's {@code DelegatingMultipartFormDataBodyParser}, but
 * overrides the parsing logic to allow for streaming the multipart form data directly to a
 * destination (e.g., file system, cloud storage)
 *
 * <p>Subclasses provide the implementation for handling the streaming, e.g. to different cloud
 * storage providers or a local file system.
 */
public abstract class StreamingMultipartBodyParser
    extends BodyParser.DelegatingMultipartFormDataBodyParser<Void> {
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

  // Override the method to create a file part handler that streams the file data to the destination
  @Override
  public Function<play.core.parsers.Multipart.FileInfo, Accumulator<ByteString, FilePart<Void>>>
      createFilePartHandler() {
    return fileInfo -> {
      String bucketName = getBucketName();
      String fileKey = getFileKey();
      Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> uploadSink =
          createUploadSink(bucketName, fileKey);

      String fileName = fileInfo.fileName();

      AtomicReference<String> detectedMimeTypeRef = new AtomicReference<>(null);
      Flow<ByteString, ByteString, ?> sniffingFlow =
          fileTypeValidation.sniffingFlow(fileName, detectedMimeTypeRef);

      // Map upload sink to an output value, prepending the sniffing flow
      Sink<ByteString, CompletionStage<FilePart<Void>>> mappedSink =
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
                          return new FilePart<Void>(
                              fileInfo.partName(),
                              fileName,
                              detectedMimeTypeRef.get(),
                              null // The actual file content is streamed, so this can be null
                              );
                        });
                  });

      // Create an accumulator that streams the file data to the mapped upload sink
      return Accumulator.fromSink(mappedSink);
    };
  }

  // Method to allow for implementations for multiple storage providers
  // Chooses between implemented Pekko connectors based on environment.
  // For available Pekko connectors, see:
  // https://pekko.apache.org/docs/pekko-connectors/1.2/index.html
  // TODO: Add support for storage providers - currently a no-op sink that discards the data.
  protected Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> createUploadSink(
      String bucketName, String fileKey) {
    // Default implementation can be a no-op sink that simply discards the data
    return uploadSinks.getSinkForCloudProvider(bucketName, fileKey, CHUNK_SIZE);
  }

  // Abstract method to provide the file path for the location of this upload in cloud storage
  protected abstract String getFileKey();

  // Abstract method to provide the destination for streaming the file, e.g., a cloud storage bucket
  protected abstract String getBucketName();
}
