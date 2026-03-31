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
          case S3, AWS_S3 -> new AwsS3MultipartUploadSinkProvider();
          default -> new NoOpMultipartUploadSinkProvider(storageServiceName);
        };
  }

  // Method to allow for implementations for multiple storage providers
  // Chooses between implemented Pekko connectors based on environment.
  // For available Pekko connectors, see:
  // https://pekko.apache.org/docs/pekko-connectors/1.2/index.html
  public Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getSinkForCloudProvider(
      String bucketName, String fileKey) {
    return uploadSinkProvider.getUploadSink(bucketName, fileKey);
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
