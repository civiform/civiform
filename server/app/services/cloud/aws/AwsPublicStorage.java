package services.cloud.aws;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.Environment;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;
import software.amazon.awssdk.regions.Region;

/** An AWS Simple Storage Service (S3) implementation of public storage. */
@Singleton
public final class AwsPublicStorage implements PublicStorageClient {
  private static final String AWS_PUBLIC_S3_BUCKET_CONF_PATH = "aws.s3.publicbucket";
  private static final String AWS_PUBLIC_S3_FILE_LIMIT_CONF_PATH = "aws.s3.publicfilelimitmb";

  private final Region region;
  private final Credentials credentials;
  private final String bucket;
  private final int fileLimitMb;
  private final Client client;

  @Inject
  public AwsPublicStorage(
      AwsRegion region, Credentials credentials, Config config, Environment environment) {
    this.region = checkNotNull(region).get();
    this.credentials = checkNotNull(credentials);
    this.bucket = checkNotNull(config).getString(AWS_PUBLIC_S3_BUCKET_CONF_PATH);
    this.fileLimitMb = checkNotNull(config).getInt(AWS_PUBLIC_S3_FILE_LIMIT_CONF_PATH);
    if (environment.isDev()) {
      client = new LocalStackClient(config);
    } else if (environment.isTest()) {
      client = new NullClient();
    } else {
      client = new AwsClient();
    }
  }

  @Override
  public StorageUploadRequest getSignedUploadRequest(
      String fileKey, String successRedirectActionLink) {
    return SimpleStorageHelpers.getSignedUploadRequest(
      credentials,
      region,
      fileLimitMb,
      bucket,
      /* actionLink= */ client.bucketAddress(),
        fileKey,
        successRedirectActionLink
);
  }

  /** Returns a direct cloud storage URL to the file with the given key. */
  @Override
  public String getDisplayUrl(String fileKey) {
    return client.bucketAddress() + "/" + fileKey;
  }

  interface Client {
    String bucketAddress();
  }

  static class NullClient implements Client {
    @Override
    public String bucketAddress() {
      return "fake-bucket-address";
    }
  }

  class AwsClient implements Client {
    @Override
    public String bucketAddress() {
      return SimpleStorageHelpers.awsActionLink(bucket, region);
    }
  }

  class LocalStackClient implements Client {
    private final Config config;

    LocalStackClient(Config config) {
      this.config = config;
    }

    @Override
    public String bucketAddress() {
      return SimpleStorageHelpers.localStackActionLink(config, bucket, region);
    }
  }
}
