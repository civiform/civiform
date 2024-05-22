package services.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import com.typesafe.config.Config;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;

/** Class providing helper methods for working with AWS Simple Storage Service (S3). */
public final class AwsStorageUtils {

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

  private static final Logger logger = LoggerFactory.getLogger(AwsStorageUtils.class);

  /**
   * Returns a signed upload request to upload a file with the given {@code fileKey} to the given
   * {@code bucketName}.
   *
   * @param useSuccessActionRedirectAsPrefix true if {@code successActionRedirect} is just a prefix
   *     of the full redirect URL, and false if {@code successActionRedirect} is an exact match to
   *     the full redirect URL. See {@link SignedS3UploadRequest#useSuccessActionRedirectAsPrefix}
   *     for more details.
   */
  public SignedS3UploadRequest getSignedUploadRequest(
      Credentials credentials,
      Region region,
      int fileLimitMb,
      String bucketName,
      String actionLink,
      String fileKey,
      String successActionRedirect,
      boolean useSuccessActionRedirectAsPrefix,
      ImmutableSet<MediaType> contentTypes) {
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
            .setSuccessActionRedirect(successActionRedirect)
            .setUseSuccessActionRedirectAsPrefix(useSuccessActionRedirectAsPrefix);
    builder.setContentTypePrefixes(
        contentTypes.stream()
            .filter(MediaType::hasWildcard)
            .collect(ImmutableSet.toImmutableSet()));
    builder.setContentTypes(
        contentTypes.stream()
            .filter(contentType -> !contentType.hasWildcard())
            .collect(ImmutableSet.toImmutableSet()));

    if (awsCredentials instanceof AwsSessionCredentials) {
      AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) awsCredentials;
      builder.setSecurityToken(sessionCredentials.sessionToken());
    }
    return builder.build();
  }

  /** Returns the endpoint to a production AWS instance. */
  public URI prodAwsEndpoint(Region region) {
    return URI.create(String.format("https://s3.%s.amazonaws.com/", region.id()));
  }

  /** Returns the action link to use when uploading or downloading to a production AWS instance. */
  public String prodAwsActionLink(String bucketName, Region region) {
    return String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region.id());
  }

  /** Returns the action link to use when uploading or downloading to LocalStack. */
  public String localStackActionLink(Config config, String bucketName, Region region) {
    try {
      String url =
          S3EndpointProvider.defaultProvider()
              .resolveEndpoint(
                  (builder) ->
                      builder
                          .endpoint(localStackEndpoint(config).toString())
                          .bucket(bucketName)
                          .region(region))
              .get()
              .url()
              .toString();
      // The prod AWS action links end with `/`, so our LocalStack action links should do the same.
      return url + "/";
    } catch (ExecutionException | InterruptedException e) {
      logger.warn("Unable to create a Localstack action link. Returning empty string");
      return "";
    }
  }

  /** Returns the endpoint to use to connect with LocalStack to manage file storage. */
  public URI localStackEndpoint(Config config) {
    String localEndpoint = checkNotNull(config).getString(AWS_LOCAL_ENDPOINT_CONF_PATH);
    // LocalStack actions that deal with file storage (upload, download, deletion) need to have
    // "s3." prepended to the URL for them to work correctly. However, we also use LocalStack
    // for non-file actions like emailing applicants when they've submitted an application (see
    // SimpleEmail). Those actions do *not* need the "s3." in the URL.
    // So, AWS_LOCAL_ENDPOINT_CONF_PATH represents the main LocalStack URL without the "s3."
    // and we manually add the "s3." here since it's only needed for file-related LocalStack
    // actions.
    URI mainUri = URI.create(localEndpoint);
    return URI.create(String.format("%s://s3.%s", mainUri.getScheme(), mainUri.getAuthority()));
  }
}
