package services.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import com.typesafe.config.Config;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import services.cloud.PublicStorageClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/** An AWS Simple Storage Service (S3) implementation of public storage. */
@Singleton
public final class AwsPublicStorage extends PublicStorageClient {
  @VisibleForTesting static final String AWS_PUBLIC_S3_BUCKET_CONF_PATH = "aws.s3.public_bucket";

  @VisibleForTesting
  static final String AWS_PUBLIC_S3_FILE_LIMIT_CONF_PATH = "aws.s3.public_file_limit_mb";

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
  public String getBucketName() {
    return bucket;
  }

  @Override
  public int getFileLimitMb() {
    return fileLimitMb;
  }

  @Override
  public SignedS3UploadRequest getSignedUploadRequest(
      String fileKey, String successRedirectActionLink, ImmutableSet<MediaType> contentTypes) {
    return awsStorageUtils.getSignedUploadRequest(
        credentials,
        region,
        fileLimitMb,
        bucket,
        /* actionLink= */ client.actionLink(),
        fileKey,
        successRedirectActionLink,
        /* useSuccessActionRedirectAsPrefix= */ false,
        contentTypes);
  }

  /** Returns a direct cloud storage URL to the file with the given key. */
  @Override
  protected String getPublicDisplayUrlInternal(String fileKey) {
    return client.actionLink() + fileKey;
  }

  @Override
  public void prunePublicFileStorage(ImmutableSet<String> inUseFileKeys) {
    List<String> unusedPublicFileKeys;
    try {
      // We should delete all the files that are in storage but not in the in-use set.
      unusedPublicFileKeys = new ArrayList<>(getCurrentFileKeys());
      unusedPublicFileKeys.removeAll(inUseFileKeys);
    } catch (FileListFailureException e) {
      // See UnusedProgramImagesCleanupJob for the deletion cadence.
      logger.error(
          "Failed to fetch the current list of public files, so aborting unused file deletion."
              + " Deletion will be re-tried again in a month. Error: {}",
          e.toString());
      return;
    }

    if (unusedPublicFileKeys.isEmpty()) {
      logger.info("No unused public files found. No files will be deleted.");
      return;
    }

    deletePublicFiles(ImmutableList.copyOf(unusedPublicFileKeys));
  }

  private ImmutableList<String> getCurrentFileKeys() throws FileListFailureException {
    return awsS3ClientWrapper.listObjects(
        credentials,
        region,
        client.endpoint(),
        ListObjectsV2Request.builder().bucket(bucket).build());
  }

  private void deletePublicFiles(ImmutableList<String> fileKeys) {
    ImmutableList<ObjectIdentifier> fileKeyObjects =
        fileKeys.stream()
            .map(key -> ObjectIdentifier.builder().key(key).build())
            .collect(ImmutableList.toImmutableList());
    DeleteObjectsRequest request =
        DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(Delete.builder().objects(fileKeyObjects).build())
            .build();
    try {
      awsS3ClientWrapper.deleteObjects(credentials, region, client.endpoint(), request);
    } catch (FileDeletionFailureException e) {
      // See UnusedProgramImagesCleanupJob for the deletion cadence.
      logger.error(
          "Some public files failed to be deleted. Deletion will be re-tried again in a month."
              + " Error: {}",
          e.toString());
    }
  }

  /** Interface defining where storage requests should be sent. */
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
     * Returns the action link that public files should be uploaded to or viewed from. Must end in a
     * `/`.
     *
     * <p>The action link *should* contain the particular bucket that files will be sent to. For
     * example, "http://civiform-local-s3-public.s3.localhost.localstack.cloud:4566/" not
     * "http://s3.localhost.localstack.cloud:4566".
     */
    String actionLink();
  }

  /** A null client implementation used for tests. */
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

  /** A real AWS client implementation used for deployments. */
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

  /** A LocalStack client implementation used for local development. */
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
