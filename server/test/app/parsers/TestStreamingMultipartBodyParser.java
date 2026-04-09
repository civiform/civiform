package parsers;

import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.cloud.MultipartUploadSinks;
import play.core.parsers.Multipart;
import play.http.DefaultHttpErrorHandler;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

// A no-op implementation of the StreamingMultipartBodyParser for testing purposes
// Prints the content of the input byte string, with a summary at the end of the total size.
public final class TestStreamingMultipartBodyParser extends StreamingMultipartBodyParser {
  private final parsers.StreamingOutputBuffer outputBuffer;

  @Inject
  public TestStreamingMultipartBodyParser(
      Materializer materializer,
      DefaultHttpErrorHandler errorHandler,
      MultipartUploadSinks streamingMultipartUploadSinks,
      FileTypeValidation fileTypeValidation,
      parsers.StreamingOutputBuffer outputBuffer) {
    long maxFileSize = 1024 * 1024 * 100L; // 100MB
    super(
        materializer, errorHandler, streamingMultipartUploadSinks, fileTypeValidation, maxFileSize);
    this.outputBuffer = outputBuffer;
  }

  // "Uploads" the file by consuming the byte string and printing its content, then returns a
  // successful result.
  @Override
  protected Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> createUploadSink(
      BucketType bucketType, String fileKey) {
    return Sink.<ByteString, ByteString>fold(
            ByteString.emptyByteString(),
            (acc, chunk) -> {
              // Print the content of the chunk (for testing purposes)
              String content = chunk.utf8String();
              outputBuffer.add(content);

              // Update the accumulated byte string
              return acc.concat(chunk);
            })
        .mapMaterializedValue(
            completionStage ->
                completionStage.thenApply(
                    fullData -> {
                      outputBuffer.add("----- Upload complete ------");
                      outputBuffer.add("Total size: " + fullData.size() + " bytes");

                      // Return a completed future with a default result, since this is just a
                      // placeholder
                      return StreamingMultipartUploadResult.builder()
                          .setStatus(StreamingMultipartUploadResult.Status.SUCCESS)
                          .setStorageServiceName(StorageServiceName.S3)
                          .build();
                    }));
  }

  // For testing purposes, we can return dummy values for the bucket name and file key

  @Override
  protected BucketType getBucketType() {
    return BucketType.PRIVATE_BUCKET;
  }

  @Override
  protected String getFileKey(Multipart.FileInfo fileInfo) {
    return "";
  }
}
