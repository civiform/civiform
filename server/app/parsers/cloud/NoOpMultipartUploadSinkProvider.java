package parsers.cloud;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.StreamingMultipartUploadResult;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

public final class NoOpMultipartUploadSinkProvider
    extends GenericMultipartUploadSinkProvider<Void> {

  public NoOpMultipartUploadSinkProvider(StorageServiceName storageServiceName) {
    super(storageServiceName);
  }

  @Override
  // No-op sink for base
  protected Sink<ByteString, CompletionStage<Void>> getBaseSink(
      BucketType bucketType, String fileKey, int chunkSize) {
    return Sink.<ByteString>ignore().mapMaterializedValue(result -> result.thenApply(done -> null));
  }

  @Override
  // Do nothing, and then return a result of NOT_IMPLEMENTED
  public Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getUploadSink(
      BucketType bucketType, String fileKey, int chunkSize) {
    return getBaseSink(bucketType, fileKey, chunkSize)
        .mapMaterializedValue(
            completionStage -> {
              // Return a completed future with no result, since this is just a placeholder
              return CompletableFuture.completedFuture(
                  StreamingMultipartUploadResult.builder()
                      .setStatus(StreamingMultipartUploadResult.Status.NOT_IMPLEMENTED)
                      .setStorageServiceName(storageServiceName)
                      .build());
            });
  }
}
