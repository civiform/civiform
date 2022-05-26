package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import annotations.FeatureFlags.StatusTrackingEnabled;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;

public class FeatureFlagsModule extends AbstractModule {
  @Override
  public void configure() {
  }

  @Provides
  @StatusTrackingEnabled
  public boolean provideStatusTrackingEnabled(Config config) {
    return checkNotNull(config).getBoolean("status_tracking_enabled");
  }

}
