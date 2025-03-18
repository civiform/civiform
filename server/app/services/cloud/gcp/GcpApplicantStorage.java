package services.cloud.gcp;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.Environment;
import play.inject.ApplicationLifecycle;
import services.cloud.StorageServiceName;
import services.cloud.aws.Credentials;
import services.cloud.generic_s3.AbstractS3ApplicantStorage;

/** An GCP Simple Storage Service (S3) extension of {@link AbstractS3ApplicantStorage}. */
@Singleton
public final class GcpApplicantStorage extends AbstractS3ApplicantStorage {

  private static final String GCP_S3_BUCKET_CONF_PATH = "gcp.s3.bucket";
  @VisibleForTesting static final String GCP_S3_FILE_LIMIT_CONF_PATH = "gcp.s3.filelimitmb";

  @Inject
  public GcpApplicantStorage(
      GcpStorageUtils gcpStorageUtils,
      GcpRegion region,
      Credentials credentials,
      Config config,
      Environment environment,
      ApplicationLifecycle appLifecycle) {
    super(gcpStorageUtils, region, credentials, config, environment, appLifecycle);
  }

  /** The bucket path defined in the conf file */
  @Override
  protected String getBucketConfigPath() {
    return GCP_S3_BUCKET_CONF_PATH;
  }

  /** The filelimitmb path in the conf file */
  @Override
  protected String getFileLimitMbPath() {
    return GCP_S3_FILE_LIMIT_CONF_PATH;
  }

  /** Gets the {@link StorageServiceName} for the current storage client. */
  @Override
  public StorageServiceName getStorageServiceName() {
    return StorageServiceName.GCP_S3;
  }
}
