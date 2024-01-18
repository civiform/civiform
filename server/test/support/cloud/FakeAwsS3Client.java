package support.cloud;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import services.cloud.aws.AwsS3ClientWrapper;
import services.cloud.aws.Credentials;
import services.cloud.aws.FileDeletionFailureException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/** A fake implementation of {@link AwsS3ClientWrapper} to be used in tests. */
public final class FakeAwsS3Client implements AwsS3ClientWrapper {
  public static final String DELETION_ERROR_FILE_KEY =
      "program-summary-image/program-8/deletion_error";

  private URI lastDeleteEndpointUsed;

  @Override
  public void deleteObjects(
      Credentials credentials, Region region, URI endpoint, DeleteObjectsRequest request)
      throws FileDeletionFailureException {
    this.lastDeleteEndpointUsed = endpoint;
    // This mimics what the real S3Client might do when certain errors occur.
    if (request
        .delete()
        .objects()
        .contains(ObjectIdentifier.builder().key(DELETION_ERROR_FILE_KEY).build())) {
      throw new FileDeletionFailureException(AwsServiceException.builder().build());
    }
  }

  @Override
  public ImmutableList<String> listObjects(
      Credentials credentials, Region region, URI endpoint, ListObjectsV2Request request) {
    return ImmutableList.of();
  }

  /** Returns the endpoint last used when calling {@link #deleteObjects}. */
  public URI getLastDeleteEndpointUsed() {
    return lastDeleteEndpointUsed;
  }
}
