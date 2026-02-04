package support.cloud;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import services.cloud.aws.Credentials;
import services.cloud.aws.FileDeletionFailureException;
import services.cloud.aws.FileListFailureException;
import services.cloud.generic_s3.GenericS3ClientWrapper;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/** A fake implementation of {@link GenericS3ClientWrapper} to be used in tests. */
public final class FakeS3Client implements GenericS3ClientWrapper {
  /**
   * A file key that, if used, will cause {@link #listObjects} to throw a {@link
   * services.cloud.aws.FileListFailureException}.
   */
  public static final String LIST_ERROR_FILE_KEY = "program-summary-image/program-8/list_error";

  /**
   * A file key that, if used, will cause {@link #deleteObjects} to throw a {@link
   * FileDeletionFailureException}.
   */
  public static final String DELETION_ERROR_FILE_KEY =
      "program-summary-image/program-8/deletion_error";

  private URI lastDeleteEndpointUsed;
  private final List<String> objects = new ArrayList<>();

  /**
   * "Adds" an object to this fake bucket. Objects added here will be returned by {@link
   * #listObjects}.
   */
  public void addObject(String objectFileKey) {
    objects.add(objectFileKey);
  }

  /**
   * "Deletes" an object from this fake bucket. Objects deleted from here will no longer be returned
   * by {@link #listObjects}.
   */
  @Override
  public void deleteObjects(
      Credentials credentials, Region region, URI endpoint, DeleteObjectsRequest request)
      throws FileDeletionFailureException {
    this.lastDeleteEndpointUsed = endpoint;
    // This mimics what the real S3Client might do when certain errors occur.
    ImmutableList<String> keys =
        request.delete().objects().stream()
            .map(ObjectIdentifier::key)
            .collect(ImmutableList.toImmutableList());
    if (keys.contains(DELETION_ERROR_FILE_KEY)) {
      throw new FileDeletionFailureException(AwsServiceException.builder().build());
    }
    request.delete().objects().forEach(object -> objects.remove(object.key()));
  }

  @Override
  public ImmutableList<String> listObjects(
      Credentials credentials, Region region, URI endpoint, ListObjectsV2Request request)
      throws FileListFailureException {
    if (objects.contains(LIST_ERROR_FILE_KEY)) {
      throw new FileListFailureException(AwsServiceException.builder().build());
    }
    return ImmutableList.copyOf(objects);
  }

  /** Returns the endpoint last used when calling {@link #deleteObjects}. */
  public URI getLastDeleteEndpointUsed() {
    return lastDeleteEndpointUsed;
  }
}
