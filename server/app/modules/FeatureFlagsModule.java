package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import annotations.FeatureFlags.AllowGlobalAdminsBeProgramAdmins;
import annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;

/** Provides configuration backed values that indicate if application wide features are enabled. */
public class FeatureFlagsModule extends AbstractModule {
  /**
   * While the Config can't change during the run of the application, in dev mode hot reloading does
   * occur.
   */
  @Provides
  @ApplicationStatusTrackingEnabled
  public boolean provideStatusTrackingEnabled(Config config) {
    return checkNotNull(config).getBoolean("application_status_tracking_enabled");
  }

  @Provides
  @AllowGlobalAdminsBeProgramAdmins
  public boolean provideAllowGlobalAdminsBeProgramAdmins(Config config) {
    return checkNotNull(config).getBoolean("allow_global_admins_be_program_admins");
  }
}
