package support.cloud;

import java.net.URI;
import services.cloud.aws.AwsS3ClientWrapper;
import services.cloud.aws.Credentials;
import services.cloud.aws.FileDeletionFailureException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

public final class FakeAwsS3Client implements AwsS3ClientWrapper {
  public static final String DELETION_ERROR_FILE_KEY =
      "program-summary-image/program-8/deletion_error";

  private URI lastEndpointUsed;

  @Override
  public void deleteObject(
      Credentials credentials, Region region, URI endpoint, DeleteObjectRequest request) throws FileDeletionFailureException {
    this.lastEndpointUsed = endpoint;
    // This mimics what the real S3Client might do when certain errors occur.
    if (request.key().equals(DELETION_ERROR_FILE_KEY)) {
      throw new FileDeletionFailureException(AwsServiceException.builder().build());
    }
  }

  public URI getLastEndpointUsed() {
    return lastEndpointUsed;
  }
}
