package services.cloud.aws;

import com.typesafe.config.Config;
import javax.inject.Inject;
import services.cloud.generic_s3.AbstractS3Region;

/** This class reads the AWS region in application.conf and builds a {@code Region} object. */
public final class AwsRegion extends AbstractS3Region {
  public static final String AWS_REGION_CONF_PATH = "aws.region";

  @Inject
  public AwsRegion(Config config) {
    super(config);
  }

  /** The region path defined in the conf file */
  @Override
  public String getRegionConfigPath() {
    return AWS_REGION_CONF_PATH;
  }
}
