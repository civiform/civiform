package featureflags;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import javax.inject.Inject;
import play.mvc.Http.Request;

/**
 * Provides configuration backed values that indicate if application wide features are enabled.
 *
 * <p>Values are primarily derived from {@link Config} with overrides allowed via the {@link
 * Request} session cookie as set by {@link controllers.dev.FeatureFlagOverrideController}.
 */
public final class FeatureFlags {
  private static final String FEATURE_FLAG_OVERRIDES_ENABLED = "feature_flag_overrides_enabled";
  public static final String APPLICATION_STATUS_TRACKING_ENABLED =
      "application_status_tracking_enabled";
  private final Config config;

  @Inject
  FeatureFlags(Config config) {
    this.config = checkNotNull(config);
  }

  private boolean areOverridesEnabled() {
    return config.getBoolean(FEATURE_FLAG_OVERRIDES_ENABLED);
  }

  /** If the Status Tracking feature is enabled. */
  public boolean isStatusTrackingEnabled(Request request) {
    return getFlagEnabled(request, APPLICATION_STATUS_TRACKING_ENABLED);
  }

  /**
   * Returns the current setting for {@param flag} from session cookie if present or then the system
   * {@link Config}.
   */
  private boolean getFlagEnabled(Request request, String flag) {
    if (!areOverridesEnabled()) {
      return false;
    }
    try {
      return request
          .session()
          .get(flag)
          .map(Boolean::parseBoolean)
          .orElseGet(() -> config.getBoolean(flag));
    } catch (ConfigException.Missing ignore) {
      // Ignore if the Config doesn't have the value configured.
      return false;
    }
  }
}
