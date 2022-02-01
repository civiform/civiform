package services.aws;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import javax.inject.Inject;
import software.amazon.awssdk.regions.Region;

/** This class reads the AWS region in application.conf and builds a {@code Region} object. */
public class AwsRegion {
  public static final String AWS_REGION_CONF_PATH = "aws.region";

  private final Region region;

  @Inject
  public AwsRegion(Config config) {
    String regionName = checkNotNull(config).getString(AWS_REGION_CONF_PATH);
    region = Region.of(regionName);
  }

  public Region get() {
    return region;
  }
}
