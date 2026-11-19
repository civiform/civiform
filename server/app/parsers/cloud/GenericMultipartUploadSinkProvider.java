package parsers.cloud;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.StreamingMultipartUploadResult;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

public abstract class GenericMultipartUploadSinkProvider<T> {
  protected final StorageServiceName storageServiceName;

  public GenericMultipartUploadSinkProvider(StorageServiceName storageServiceName) {
    this.storageServiceName = storageServiceName;
  }

  // Get the composed sink to upload to the cloud storage provider, returning a custom result class.
  public abstract Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getUploadSink(
      BucketType bucketType,
      String fileKey,
      Optional<String> contentType,
      Optional<String> contentDisposition,
      int chunkSize);

  public Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getUploadSink(
      BucketType bucketType, String fileKey, int chunkSize) {
    return getUploadSink(bucketType, fileKey, Optional.empty(), Optional.empty(), chunkSize);
  }

  // Split out the call to cloud storage providers for improved testability
  protected abstract Sink<ByteString, CompletionStage<T>> getBaseSink(
      BucketType bucketType,
      String fileKey,
      Optional<String> contentType,
      Optional<String> contentDisposition,
      int chunkSize);

  protected Sink<ByteString, CompletionStage<T>> getBaseSink(
      BucketType bucketType, String fileKey, int chunkSize) {
    return getBaseSink(bucketType, fileKey, Optional.empty(), Optional.empty(), chunkSize);
  }

  // Construct a failed upload result for the given storage service from a supplied throwable.
  protected StreamingMultipartUploadResult failedResult(Throwable throwable) {
    Throwable cause = unwrapAsyncWrapper(throwable);
    String errorMessage = cause.getMessage();
    if (errorMessage == null && cause.getCause() != null) {
      errorMessage = cause.getCause().getMessage();
    }

    return StreamingMultipartUploadResult.builder()
        .setStatus(StreamingMultipartUploadResult.Status.FAILURE)
        .setStorageServiceName(storageServiceName)
        .setErrorMessage(Optional.ofNullable(errorMessage))
        .setCause(Optional.of(cause))
        .build();
  }

  private static Throwable unwrapAsyncWrapper(Throwable throwable) {
    Throwable current = throwable;
    while ((current instanceof CompletionException || current instanceof ExecutionException)
        && current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }
}
