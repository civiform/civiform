package services.cloud.aws;

import java.net.URI;
import java.time.Duration;
import services.cloud.generic_s3.AbstractS3StorageUtils;
import software.amazon.awssdk.regions.Region;

/** Class providing helper methods for working with AWS Simple Storage Service (S3). */
public final class AwsStorageUtils extends AbstractS3StorageUtils {

  /**
   * The duration that a signed upload or download request URL from {@link #getSignedUploadRequest}
   * is valid for use.
   */
  public static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(10);

  @Override
  protected Duration getPresignedUrlDuration() {
    return PRESIGNED_URL_DURATION;
  }

  /** Returns the endpoint to a production AWS instance. */
  @Override
  public URI prodEndpoint(Region region) {
    return URI.create(String.format("https://s3.%s.amazonaws.com/", region.id()));
  }

  /** Returns the action link to use when uploading or downloading to a production AWS instance. */
  @Override
  public String prodActionLink(String bucketName, Region region) {
    return String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region.id());
  }
}
