package parsers;

import static helpers.FileTypeValidator.detectFileType;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.connectors.s3.MultipartUploadResult;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import play.http.DefaultHttpErrorHandler;
import play.libs.streams.Accumulator;
import play.mvc.BodyParser;
import play.mvc.Http.MultipartFormData.FilePart;
import services.cloud.ApplicantFileNameFormatter;
import services.cloud.PublicStorageClient;

public abstract class StreamingMultipartBodyParser
    extends BodyParser.DelegatingMultipartFormDataBodyParser<Void> {
  private static final long HEADER_SIZE = 16; // 16 bytes for file headers

  private final PublicStorageClient publicStorageClient;

  public StreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      PublicStorageClient publicStorageClient) {
    super(materializer, 1024 * 1024, 1024 * 1024 * 500, /* allowEmptyFiles= */ false, errorHandler);
    this.publicStorageClient = publicStorageClient;
  }

  @Override
  public Function<play.core.parsers.Multipart.FileInfo, Accumulator<ByteString, FilePart<Void>>>
      createFilePartHandler() {
    return fileInfo -> {
      String bucketName = publicStorageClient.getBucketName();
      String key =
          ApplicantFileNameFormatter.formatFileUploadQuestionFilename(
              0L, 0L, "block-id-placeholder");
      // Holds captured header bytes
      AtomicReference<ByteString> headerBytes =
          new AtomicReference<>(
              ByteString.emptyByteString()); // Thread-safe storage for header bytes

      // Sniff first few bytes for file type detection
      Flow<ByteString, ByteString, ?> sniffingFlow =
          Flow.of(ByteString.class)
              .map(
                  bytes -> {
                    ByteString currentHeader = headerBytes.get();

                    if (currentHeader.size() < HEADER_SIZE) {
                      int remaining = (int) (HEADER_SIZE - currentHeader.size());
                      ByteString slice = bytes.take(remaining);
                      headerBytes.set(currentHeader.concat(slice));
                    }

                    return bytes;
                  });

      // Compose the sniffing flow with the actual upload sink to create a single sink that handles
      // both header detection and file upload
      Sink<ByteString, CompletionStage<MultipartUploadResult>> composedSink =
          sniffingFlow.toMat(createUploadSink(bucketName, key), Keep.right());

      return Accumulator.fromSink(
          composedSink.mapMaterializedValue(
              uploadResultFuture ->
                  uploadResultFuture.thenApply(
                      uploadResult -> {
                        ByteString headerData = headerBytes.get();

                        return new FilePart<>(
                            fileInfo.partName(),
                            fileInfo.fileName(),
                            detectFileType(headerData),
                            null);
                      })));
    };
  }

  protected abstract Sink<ByteString, CompletionStage<MultipartUploadResult>> createUploadSink(
      String bucketName, String key);
}
