package parsers.cloud;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.BadValue;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.pekko.stream.connectors.s3.MultipartUploadResult;
import org.apache.pekko.stream.connectors.s3.javadsl.S3;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.StreamingMultipartUploadResult;
import services.cloud.StorageServiceName;

public final class StreamingMultipartUploadSinks {
  private final Config config;

  @Inject
  public StreamingMultipartUploadSinks(Config config) {
    this.config = checkNotNull(config);
  }

  // Method to allow for implementations for multiple storage providers
  // Chooses between implemented Pekko connectors based on environment.
  // For available Pekko connectors, see:
  // https://pekko.apache.org/docs/pekko-connectors/1.2/index.html
  public Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getSinkForCloudProvider(
      String bucketName, String fileKey) {
    StorageServiceName storageServiceName = getStorageService();
    return switch (storageServiceName) {
      case S3, AWS_S3 -> getAwsS3UploadSink(bucketName, fileKey);
      default -> getDefaultNoOpUploadSink(storageServiceName);
    };
  }

  // Splitting out the S3 call, so that this can be mocked in unit tests
  Sink<ByteString, CompletionStage<MultipartUploadResult>> getBaseS3Sink(
      String bucketName, String fileKey) {
    return S3.multipartUpload(bucketName, fileKey);
  }

  // Return a sink that uploads to AWS S3, and interprets the MultipartUploadResult.
  private Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getAwsS3UploadSink(
      String bucketName, String fileKey) {
    return getBaseS3Sink(bucketName, fileKey)
        .mapMaterializedValue(
            completionStage ->
                completionStage
                    .thenApply(
                        awsResult -> {
                          return StreamingMultipartUploadResult.builder()
                              .setStatus(StreamingMultipartUploadResult.Status.SUCCESS)
                              .setStorageServiceName(StorageServiceName.AWS_S3)
                              .setStoredFilePath(Optional.of(awsResult.getKey()))
                              .build();
                        })
                    .exceptionally(
                        throwable -> {
                          return failedResult(throwable, StorageServiceName.AWS_S3);
                        }));
  }

  // Default implementation is just a no-op sink that discards the data
  private Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>>
      getDefaultNoOpUploadSink(StorageServiceName storageServiceName) {
    return Sink.<ByteString>ignore()
        .mapMaterializedValue(
            completionStage -> {
              // Return a completed future with no result, since this is just a placeholder
              return CompletableFuture.completedFuture(
                  StreamingMultipartUploadResult.builder()
                      .setStatus(StreamingMultipartUploadResult.Status.INVALID_STATUS)
                      .setStorageServiceName(storageServiceName)
                      .build());
            });
  }

  // Construct a failed upload result for the given storage service from a supplied throwable.
  private StreamingMultipartUploadResult failedResult(
      Throwable throwable, StorageServiceName storageServiceName) {
    String errorMessage =
        throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();

    return StreamingMultipartUploadResult.builder()
        .setStatus(StreamingMultipartUploadResult.Status.FAILURE)
        .setStorageServiceName(storageServiceName)
        .setErrorMessage(Optional.of(errorMessage))
        .build();
  }

  // Return the StorageServiceName for the currently selected cloud storage provider
  private StorageServiceName getStorageService() {
    String storageProvider = checkNotNull(config).getString("cloud.storage");
    return StorageServiceName.forString(storageProvider)
        .orElseThrow(
            () ->
                new BadValue(
                    "cloud.storage",
                    String.format("%s is not a valid storage provider.", storageProvider)));
  }
}
