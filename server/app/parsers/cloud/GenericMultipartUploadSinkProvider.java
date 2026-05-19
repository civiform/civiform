package parsers.cloud;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.StreamingMultipartUploadResult;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

public abstract class GenericMultipartUploadSinkProvider<T> {
  protected final StorageServiceName storageServiceName;
  protected final Config config;

  public GenericMultipartUploadSinkProvider(StorageServiceName storageServiceName, Config config) {
    this.storageServiceName = storageServiceName;
    this.config = checkNotNull(config);
  }

  public long getMaxUploadSizeBytes(BucketType bucketType) {
    int limitMb = config.getInt(getFileLimitMbConfigPath(bucketType));
    return limitMb * 1024L * 1024L;
  }

  private String getFileLimitMbConfigPath(BucketType bucketType) {
    return switch (storageServiceName) {
      case S3, AWS_S3 ->
          switch (bucketType) {
            case PRIVATE_BUCKET -> "aws.s3.filelimitmb";
            case PUBLIC_BUCKET -> "aws.s3.public_file_limit_mb";
          };
      case GCP_S3 ->
          switch (bucketType) {
            case PRIVATE_BUCKET -> "gcp.s3.filelimitmb";
            case PUBLIC_BUCKET -> "gcp.s3.public_file_limit_mb";
          };
      case AZURE_BLOB ->
          switch (bucketType) {
            case PRIVATE_BUCKET -> "azure.blob.file_limit_mb";
            case PUBLIC_BUCKET -> "azure.blob.public_file_limit_mb";
          };
    };
  }

  // Get the composed sink to upload to the cloud storage provider, returning a custom result class.
  public abstract Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getUploadSink(
      BucketType bucketType, String fileKey, int chunkSize);

  // Split out the call to cloud storage providers for improved testability
  protected abstract Sink<ByteString, CompletionStage<T>> getBaseSink(
      BucketType bucketType, String fileKey, int chunkSize);

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
