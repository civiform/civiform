package featureflags;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http.Request;

/**
 * Provides configuration backed values that indicate if application wide features are enabled.
 *
 * <p>Values are primarily derived from {@link Config} with overrides allowed via the {@link
 * Request} session cookie as set by {@link controllers.dev.FeatureFlagOverrideController}.
 */
public final class FeatureFlags {
  private static final Logger logger = LoggerFactory.getLogger(FeatureFlags.class);
  private static final String FEATURE_FLAG_OVERRIDES_ENABLED = "feature_flag_overrides_enabled";
  public static final String APPLICATION_STATUS_TRACKING_ENABLED =
      "application_status_tracking_enabled";
  private final Config config;

  @Inject
  FeatureFlags(Config config) {
    this.config = checkNotNull(config);
  }

  public boolean areOverridesEnabled() {
    return config.hasPath(FEATURE_FLAG_OVERRIDES_ENABLED)
        && config.getBoolean(FEATURE_FLAG_OVERRIDES_ENABLED);
  }

  /** If the Status Tracking feature is enabled. */
  public boolean isStatusTrackingEnabled(Request request) {
    return getFlagEnabled(request, APPLICATION_STATUS_TRACKING_ENABLED);
  }

  /**
   * Returns the current setting for {@code flag} from {@link Config} if present, allowing for an
   * overriden value from the session cookie.
   */
  private boolean getFlagEnabled(Request request, String flag) {
    if (!config.hasPath(flag)) {
      logger.warn("Feature flag requested for unconfigured flag: {}", flag);
      return false;
    }
    Boolean configValue = config.getBoolean(flag);

    if (!areOverridesEnabled()) {
      return configValue;
    }

    Optional<Boolean> sessionValue = request.session().get(flag).map(Boolean::parseBoolean);
    if (sessionValue.isPresent()) {
      logger.warn("Returning override ({}) for feature flag: {}", sessionValue.get(), flag);
      return sessionValue.get();
    }
    return configValue;
  }
}
