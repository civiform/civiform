package parsers.cloud;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.BadValue;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.aws.AwsS3MultipartUploadSinkProvider;
import parsers.cloud.gcp.GcpMultipartUploadSinkProvider;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

public final class MultipartUploadSinks {
  private final Config config;

  final GenericMultipartUploadSinkProvider<?> uploadSinkProvider;

  @Inject
  public MultipartUploadSinks(Config config) {
    this.config = checkNotNull(config);
    StorageServiceName storageServiceName = getStorageService();
    uploadSinkProvider =
        switch (storageServiceName) {
          case S3, AWS_S3 -> new AwsS3MultipartUploadSinkProvider(config);
          case GCP_S3 -> new GcpMultipartUploadSinkProvider(config);
          default -> new NoOpMultipartUploadSinkProvider(storageServiceName);
        };
  }

  // Method to allow for implementations for multiple storage providers
  // Chooses between implemented Pekko connectors based on environment.
  // For available Pekko connectors, see:
  // https://pekko.apache.org/docs/pekko-connectors/1.2/index.html
  public Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getSinkForCloudProvider(
      BucketType bucketType, String fileKey, int chunkSize) {
    return uploadSinkProvider.getUploadSink(bucketType, fileKey, chunkSize);
  }

  // Return the StorageServiceName for the currently selected cloud storage provider
  private StorageServiceName getStorageService() {
    String storageProvider = config.getString("cloud.storage");
    return StorageServiceName.forString(storageProvider)
        .orElseThrow(
            () ->
                new BadValue(
                    "cloud.storage",
                    String.format("%s is not a valid storage provider.", storageProvider)));
  }

  // For testing
  GenericMultipartUploadSinkProvider<?> getUploadSinkProvider() {
    return uploadSinkProvider;
  }
}
