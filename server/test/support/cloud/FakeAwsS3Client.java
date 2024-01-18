package support.cloud;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
  private final List<String> objects = new ArrayList<>();

  /**
   * "Deletes" an object from the AWS bucket. Objects deleted from here will no longer be returned
   * by {@link #listObjects}.
   */
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
    request.delete().objects().forEach(object -> objects.remove(object.key()));
  }

  @Override
  public ImmutableList<String> listObjects(
      Credentials credentials, Region region, URI endpoint, ListObjectsV2Request request) {
    return ImmutableList.copyOf(objects);
  }

  /**
   * "Adds" an object to the AWS bucket. Objects added here will be returned by {@link
   * #listObjects}.
   */
  public void addObject(String objectFileKey) {
    objects.add(objectFileKey);
  }

  /** Returns the endpoint last used when calling {@link #deleteObjects}. */
  public URI getLastDeleteEndpointUsed() {
    return lastDeleteEndpointUsed;
  }
}
