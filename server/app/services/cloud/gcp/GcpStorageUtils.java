package services.cloud.gcp;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.cloud.generic_s3.AbstractS3StorageUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;

/** Class providing helper methods for working with GCP Simple Storage Service (S3). */
public final class GcpStorageUtils extends AbstractS3StorageUtils {
  private static final Logger logger = LoggerFactory.getLogger(GcpStorageUtils.class);

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
    return URI.create("https://storage.googleapis.com/");
  }

  /** Returns the action link to use when uploading or downloading to a production AWS instance. */
  @Override
  public String prodActionLink(String bucketName, Region region) {
    try {
      return S3EndpointProvider.defaultProvider()
          .resolveEndpoint(
              (builder) ->
                  builder
                      .endpoint("https://storage.googleapis.com/")
                      .bucket(bucketName)
                      .region(region))
          .get()
          .url()
          .toString();
    } catch (ExecutionException | InterruptedException e) {
      logger.warn("Unable to create an S3 action link. Return empty string.", e);
      return "";
    }
  }
}
