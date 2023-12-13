package services.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;

/** Class providing helper methods for working with AWS Simple Storage Service (S3). */
public final class AwsStorageUtils {
  private AwsStorageUtils() {}
  /**
   * The duration that a signed upload or download request URL from {@link #getSignedUploadRequest}
   * is valid for use.
   */
  public static final Duration AWS_PRESIGNED_URL_DURATION = Duration.ofMinutes(10);

  /**
   * The path to the config variable containing the endpoint for our local AWS instance
   * (LocalStack).
   */
  public static final String AWS_LOCAL_ENDPOINT_CONF_PATH = "aws.local.endpoint";

  /**
   * Returns a signed upload request to upload a file with the given {@code fileKey} to the given
   * {@code bucketName}.
   */
  public static SignedS3UploadRequest getSignedUploadRequest(
      Credentials credentials,
      Region region,
      int fileLimitMb,
      String bucketName,
      String actionLink,
      String fileKey,
      String successRedirectActionLink) {
    AwsCredentials awsCredentials = credentials.getCredentials();
    SignedS3UploadRequest.Builder builder =
        SignedS3UploadRequest.builder()
            .setExpirationDuration(AWS_PRESIGNED_URL_DURATION)
            .setAccessKey(awsCredentials.accessKeyId())
            .setSecretKey(awsCredentials.secretAccessKey())
            .setRegionName(region.id())
            .setFileLimitMb(fileLimitMb)
            .setBucket(bucketName)
            .setActionLink(actionLink)
            .setKey(fileKey)
            .setSuccessActionRedirect(successRedirectActionLink);

    if (awsCredentials instanceof AwsSessionCredentials) {
      AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) awsCredentials;
      builder.setSecurityToken(sessionCredentials.sessionToken());
    }
    return builder.build();
  }

  /** Returns the action link to use when uploading or downloading to a production AWS instance. */
  public static String prodAwsActionLink(String bucketName, Region region) {
    return String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region.id());
  }

  /** Returns the action link to use when uploading or downloading to LocalStack. */
  public static String localStackActionLink(Config config, String bucketName, Region region) {
    try {
      return S3EndpointProvider.defaultProvider()
          .resolveEndpoint(
              (builder) ->
                  builder.endpoint(localStackEndpoint(config)).bucket(bucketName).region(region))
          .get()
          .url()
          .toString();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /** Returns the endpoint to use to connect with LocalStack. */
  public static String localStackEndpoint(Config config) {
    String localEndpoint = checkNotNull(config).getString(AWS_LOCAL_ENDPOINT_CONF_PATH);
    try {
      URI localUri = new URI(localEndpoint);
      return String.format("%s://s3.%s", localUri.getScheme(), localUri.getAuthority());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
