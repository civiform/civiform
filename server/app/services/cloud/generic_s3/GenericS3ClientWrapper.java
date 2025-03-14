package services.cloud.generic_s3;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import services.cloud.aws.Credentials;
import services.cloud.aws.FileDeletionFailureException;
import services.cloud.aws.FileListFailureException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

/**
 * A wrapper around AWS's {@link software.amazon.awssdk.services.s3.S3Client} so that we can fake it
 * out in tests.
 */
public interface GenericS3ClientWrapper {
  /**
   * Deletes objects from AWS S3 cloud storage. The objects to delete are specified in the {@code
   * request}.
   *
   * @throws IllegalArgumentException if the {@code request} doesn't specify a bucket.
   * @throws IllegalArgumentException if the {@code request.delete()} doesn't contain any objects to
   *     delete.
   * @throws FileDeletionFailureException if there was a problem deleting the file.
   */
  void deleteObjects(
      Credentials credentials, Region region, URI endpoint, DeleteObjectsRequest request)
      throws FileDeletionFailureException;

  /**
   * Returns a list of keys for all files stored in the bucket specified by {@code request}.
   *
   * @throws FileListFailureException if there was an error fetching the keys for any reason.
   */
  ImmutableList<String> listObjects(
      Credentials credentials, Region region, URI endpoint, ListObjectsV2Request request)
      throws FileListFailureException;
}
