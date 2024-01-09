package services.cloud.aws;

import com.google.inject.Inject;
import java.net.URI;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/**
 * A real implementation of {@link AwsS3ClientWrapper} that delegates to the real {@link S3Client}.
 */
public class RealAwsS3Client implements AwsS3ClientWrapper {
  @Inject
  public RealAwsS3Client() {}

  @Override
  public void deleteObject(
      Credentials credentials, Region region, URI endpoint, DeleteObjectRequest request) {
    if (request.key().isBlank()) {
      throw new IllegalArgumentException("The request must have a key.");
    }
    if (request.bucket().isBlank()) {
      throw new IllegalArgumentException("The request must have a bucket.");
    }
    try (S3Client s3Client =
        S3Client.builder()
            .credentialsProvider(credentials.credentialsProvider())
            .region(region)
            // Override the endpoint so that Localstack works correctly.
            // See https://docs.localstack.cloud/user-guide/integrations/sdks/java/#s3-service and
            // https://github.com/localstack/localstack/issues/5209#issuecomment-1004395805.
            .endpointOverride(endpoint)
            .build()) {
      s3Client.deleteObject(request);
    }
  }
}
