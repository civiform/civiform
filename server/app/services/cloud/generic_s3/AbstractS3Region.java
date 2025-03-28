package services.cloud.generic_s3;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import software.amazon.awssdk.regions.Region;

/** This class reads the region in application.conf and builds a {@code Region} object. */
public abstract class AbstractS3Region {
  private final Region region;

  public AbstractS3Region(Config config) {
    String regionName = checkNotNull(config).getString(getRegionConfigPath());
    region = Region.of(regionName);
  }

  /** The region path defined in the conf file */
  public abstract String getRegionConfigPath();

  public Region get() {
    return region;
  }
}
