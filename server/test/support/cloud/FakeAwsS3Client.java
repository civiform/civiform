package support.cloud;

import java.net.URI;
import services.cloud.aws.AwsS3ClientWrapper;
import services.cloud.aws.Credentials;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

public final class FakeAwsS3Client implements AwsS3ClientWrapper {
  public static final String PROCESS_ERROR_FILE_KEY =
      "program-summary-image/program-8/process_error";
  public static final String RESPONSE_PARSE_ERROR_FILE_KEY =
      "program-summary-image/program-9/response_parse_error";

  private URI lastEndpointUsed;

  @Override
  public void deleteObject(
      Credentials credentials, Region region, URI endpoint, DeleteObjectRequest request) {
    this.lastEndpointUsed = endpoint;
    // This mimics what the real S3Client might do when certain errors occur.
    if (request.key().equals(PROCESS_ERROR_FILE_KEY)) {
      throw AwsServiceException.builder().build();
    } else if (request.key().equals(RESPONSE_PARSE_ERROR_FILE_KEY)) {
      throw SdkClientException.builder().build();
    }
  }

  public URI getLastEndpointUsed() {
    return lastEndpointUsed;
  }
}
