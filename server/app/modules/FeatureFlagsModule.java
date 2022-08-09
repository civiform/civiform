package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import annotations.FeatureFlagOverrides;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides configuration backed values that indicate if application wide features are enabled. */
public class FeatureFlagsModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(FeatureFlagsModule.class);
  /**
   * While the Config can't change during the run of the application, in dev mode hot reloading does
   * occur.
   */
  @Provides
  @ApplicationStatusTrackingEnabled
  public boolean provideStatusTrackingEnabled(Config config, FeatureFlagOverrides overrides) {
    logger.error("provideStatusTrackingEnabled called");
    String key = "application_status_tracking_enabled";
    checkNotNull(config);
    checkNotNull(overrides);
    logger.error("Override: {}", overrides.getOverrideBoolean(key));
    logger.error("Config: {} ", config.getBoolean(key));
    return overrides.getOverrideBoolean(key).orElseGet(() -> config.getBoolean(key));
  }
}
