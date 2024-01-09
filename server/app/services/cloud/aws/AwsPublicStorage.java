package services.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import services.cloud.PublicStorageClient;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/** An AWS Simple Storage Service (S3) implementation of public storage. */
@Singleton
public final class AwsPublicStorage extends PublicStorageClient {
  private static final String AWS_PUBLIC_S3_BUCKET_CONF_PATH = "aws.s3.public_bucket";
  private static final String AWS_PUBLIC_S3_FILE_LIMIT_CONF_PATH = "aws.s3.public_file_limit_mb";

  private static final Logger logger = LoggerFactory.getLogger(AwsPublicStorage.class);

  private final AwsS3ClientWrapper awsS3ClientWrapper;
  private final AwsStorageUtils awsStorageUtils;
  private final Region region;
  private final Credentials credentials;
  private final String bucket;
  private final int fileLimitMb;
  private final Client client;

  @Inject
  public AwsPublicStorage(
      AwsS3ClientWrapper awsS3ClientWrapper,
      AwsStorageUtils awsStorageUtils,
      AwsRegion region,
      Credentials credentials,
      Config config,
      Environment environment) {
    this.awsS3ClientWrapper = checkNotNull(awsS3ClientWrapper);
    this.awsStorageUtils = checkNotNull(awsStorageUtils);
    this.region = checkNotNull(region).get();
    this.credentials = checkNotNull(credentials);
    this.bucket = checkNotNull(config).getString(AWS_PUBLIC_S3_BUCKET_CONF_PATH);
    this.fileLimitMb = checkNotNull(config).getInt(AWS_PUBLIC_S3_FILE_LIMIT_CONF_PATH);
    if (environment.isDev()) {
      client = new LocalStackClient(config, awsStorageUtils);
    } else if (environment.isProd()) {
      client = new AwsClient();
    } else {
      client = new NullClient();
    }
  }

  @Override
  public SignedS3UploadRequest getSignedUploadRequest(
      String fileKey, String successRedirectActionLink) {
    return awsStorageUtils.getSignedUploadRequest(
        credentials,
        region,
        fileLimitMb,
        bucket,
        /* actionLink= */ client.actionLink(),
        fileKey,
        successRedirectActionLink);
  }

  /** Returns a direct cloud storage URL to the file with the given key. */
  @Override
  protected String getPublicDisplayUrlInternal(String fileKey) {
    return client.actionLink() + fileKey;
  }

  @Override
  protected boolean deletePublicFileInternal(String fileKey) {
    try {
      awsS3ClientWrapper.deleteObject(
          credentials,
          region,
          client.endpoint(),
          DeleteObjectRequest.builder().bucket(bucket).key(fileKey).build());
      return true;
    } catch (AwsServiceException | SdkClientException e) {
      // AwsServiceException: The call was transmitted successfully, but AWS S3 couldn't process it
      // for some reason.
      // SdkClientException: AWS S3 couldn't be contacted for a response or the client couldn't
      // parse the response from AWS S3.
      // See https://docs.aws.amazon.com/AmazonS3/latest/userguide/delete-objects.html.
      logger.error(e.toString());
      return false;
    }
  }

  /**
   * Interface defining where storage requests should be sent:
   *
   * <ol>
   *   <li>Null (for testing)
   *   <li>LocalStack (for local development)
   *   <li>AWS (for deployments)
   * </ol>
   */
  interface Client {
    /**
     * Returns the endpoint that this client represents.
     *
     * <p>This endpoint URI should *not* include any particular bucket, and instead should just link
     * to the client's base URL. For example, "http://s3.localhost.localstack.cloud:4566" not
     * "http://civiform-local-s3-public.s3.localhost.localstack.cloud:4566/".
     */
    URI endpoint();

    /**
     * Returns the action link that public files should be sent to. Must end in a `/`.
     *
     * <p>The action link *should* contain the particular bucket that files will be sent to. For
     * example, "http://civiform-local-s3-public.s3.localhost.localstack.cloud:4566/" not
     * "http://s3.localhost.localstack.cloud:4566".
     */
    String actionLink();
  }

  static class NullClient implements Client {
    @Override
    public URI endpoint() {
      return URI.create("http://fake-endpoint.com");
    }

    @Override
    public String actionLink() {
      return "fake-action-link/";
    }
  }

  class AwsClient implements Client {
    @Override
    public URI endpoint() {
      return awsStorageUtils.prodAwsEndpoint(region);
    }

    @Override
    public String actionLink() {
      return awsStorageUtils.prodAwsActionLink(bucket, region);
    }
  }

  class LocalStackClient implements Client {
    private final Config config;
    private final AwsStorageUtils awsStorageUtils;

    LocalStackClient(Config config, AwsStorageUtils awsStorageUtils) {
      this.config = checkNotNull(config);
      this.awsStorageUtils = checkNotNull(awsStorageUtils);
    }

    @Override
    public URI endpoint() {
      return awsStorageUtils.localStackEndpoint(config);
    }

    @Override
    public String actionLink() {
      return awsStorageUtils.localStackActionLink(config, bucket, region);
    }
  }
}
