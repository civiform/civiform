package parsers.cloud;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.StreamingMultipartUploadResult;
import services.cloud.StorageServiceName;

public abstract class GenericMultipartUploadSinkProvider<T> {
  protected final StorageServiceName storageServiceName;

  public GenericMultipartUploadSinkProvider(StorageServiceName storageServiceName) {
    this.storageServiceName = storageServiceName;
  }

  // Get the composed sink to upload to the cloud storage provider, returning a custom result class.
  public abstract Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getUploadSink(
      String bucketName, String fileKey);

  // Split out the call to cloud storage providers for improved testability
  protected abstract Sink<ByteString, CompletionStage<T>> getBaseSink(
      String bucketName, String fileKey);

  // Construct a failed upload result for the given storage service from a supplied throwable.
  protected StreamingMultipartUploadResult failedResult(Throwable throwable) {
    String errorMessage =
        throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();

    return StreamingMultipartUploadResult.builder()
        .setStatus(StreamingMultipartUploadResult.Status.FAILURE)
        .setStorageServiceName(storageServiceName)
        .setErrorMessage(Optional.of(errorMessage))
        .build();
  }
}
