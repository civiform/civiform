package services.cloud.aws;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.Environment;
import services.cloud.generic_s3.AbstractS3PublicStorage;
import services.cloud.generic_s3.GenericS3ClientWrapper;

/**
 * An AWS Simple Storage Service (S3) implementation of public storage of {@link
 * AbstractS3PublicStorage}.
 */
@Singleton
public final class AwsPublicStorage extends AbstractS3PublicStorage {
  @VisibleForTesting static final String AWS_PUBLIC_S3_BUCKET_CONF_PATH = "aws.s3.public_bucket";

  @VisibleForTesting
  static final String AWS_PUBLIC_S3_FILE_LIMIT_CONF_PATH = "aws.s3.public_file_limit_mb";

  @Inject
  public AwsPublicStorage(
      GenericS3ClientWrapper awsS3ClientWrapper,
      AwsStorageUtils awsStorageUtils,
      AwsRegion region,
      Credentials credentials,
      Config config,
      Environment environment) {
    super(awsS3ClientWrapper, awsStorageUtils, region, credentials, config, environment);
  }

  /** The bucket path defined in the conf file */
  @Override
  protected String getBucketConfigPath() {
    return AWS_PUBLIC_S3_BUCKET_CONF_PATH;
  }

  /** The filelimitmb path in the conf file */
  @Override
  protected String getFileLimitMbPath() {
    return AWS_PUBLIC_S3_FILE_LIMIT_CONF_PATH;
  }
}
