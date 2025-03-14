package services.cloud.generic_s3;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import services.cloud.PublicStorageClient;
import services.cloud.aws.Credentials;
import services.cloud.aws.FileDeletionFailureException;
import services.cloud.aws.FileListFailureException;
import services.cloud.aws.SignedS3UploadRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/** An Simple Storage Service (S3) implementation of public storage. */
public abstract class AbstractS3PublicStorage extends PublicStorageClient {
  private static final Logger logger = LoggerFactory.getLogger(AbstractS3PublicStorage.class);

  private final GenericS3ClientWrapper awsS3ClientWrapper;
  private final AbstractS3StorageUtils awsStorageUtils;
  private final Region region;
  private final Credentials credentials;
  private final String bucket;
  private final int fileLimitMb;
  private final AbstractS3PublicStorage.Client client;

  public AbstractS3PublicStorage(
      GenericS3ClientWrapper awsS3ClientWrapper,
      AbstractS3StorageUtils awsStorageUtils,
      AbstractS3Region region,
      Credentials credentials,
      Config config,
      Environment environment) {
    this.awsS3ClientWrapper = checkNotNull(awsS3ClientWrapper);
    this.awsStorageUtils = checkNotNull(awsStorageUtils);
    this.region = checkNotNull(region).get();
    this.credentials = checkNotNull(credentials);
    this.bucket = checkNotNull(config).getString(getBucketConfigPath());
    this.fileLimitMb = checkNotNull(config).getInt(getFileLimitMbPath());
    if (environment.isDev()) {
      client = new AbstractS3PublicStorage.LocalStackClient(config, awsStorageUtils);
    } else if (environment.isProd()) {
      client = new AbstractS3PublicStorage.AwsClient();
    } else {
      client = new AbstractS3PublicStorage.NullClient();
    }
  }

  /** The bucket path defined in the conf file */
  protected abstract String getBucketConfigPath();

  /** The filelimitmb path in the conf file */
  protected abstract String getFileLimitMbPath();

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
      String fileKey, String successRedirectActionLink) {
    return awsStorageUtils.getSignedUploadRequest(
        credentials,
        region,
        fileLimitMb,
        bucket,
        /* actionLink= */ client.actionLink(),
        fileKey,
        successRedirectActionLink,
        /* useSuccessActionRedirectAsPrefix= */ false);
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
  static class NullClient implements AbstractS3PublicStorage.Client {
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
  class AwsClient implements AbstractS3PublicStorage.Client {
    @Override
    public URI endpoint() {
      return awsStorageUtils.prodEndpoint(region);
    }

    @Override
    public String actionLink() {
      return awsStorageUtils.prodActionLink(bucket, region);
    }
  }

  /** A LocalStack client implementation used for local development. */
  class LocalStackClient implements AbstractS3PublicStorage.Client {
    private final Config config;
    private final AbstractS3StorageUtils awsStorageUtils;

    LocalStackClient(Config config, AbstractS3StorageUtils awsStorageUtils) {
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
