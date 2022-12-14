package featureflags;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
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
  // Main control for any feature flags working.
  private static final String FEATURE_FLAG_OVERRIDES_ENABLED = "feature_flag_overrides_enabled";

  // Long lived feature flags.
  public static final String ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS =
      "allow_civiform_admin_access_programs";

  // Launch Flags, these will eventually be removed.
  public static final String APPLICATION_STATUS_TRACKING_ENABLED =
      "application_status_tracking_enabled";
  public static final String PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED =
      "program_eligibility_conditions_enabled";
  private static final String USE_JS_BUNDLES = "use_js_bundles";

  private final Config config;

  @Inject
  FeatureFlags(Config config) {
    this.config = checkNotNull(config);
  }

  public boolean areOverridesEnabled() {
    return config.hasPath(FEATURE_FLAG_OVERRIDES_ENABLED)
        && config.getBoolean(FEATURE_FLAG_OVERRIDES_ENABLED);
  }

  /**
   * If the Eligibility Conditions feature is enabled.
   *
   * <p>Allows for overrides set in {@code request}.
   */
  public boolean isProgramEligibilityConditionsEnabled(Request request) {
    return getFlagEnabled(request, PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED);
  }

  /** If the Eligibility Conditions feature is enabled in the system configuration. */
  public boolean isProgramEligibilityConditionsEnabled() {
    return config.getBoolean(PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED);
  }

  /**
   * If the Status Tracking feature is enabled.
   *
   * <p>Allows for overrides set in {@code request}.
   */
  public boolean isStatusTrackingEnabled(Request request) {
    return getFlagEnabled(request, APPLICATION_STATUS_TRACKING_ENABLED);
  }

  /** If the Status Tracking feature is enabled in the system configuration. */
  public boolean isStatusTrackingEnabled() {
    return config.getBoolean(APPLICATION_STATUS_TRACKING_ENABLED);
  }

  public boolean allowCiviformAdminAccessPrograms(Request request) {
    return getFlagEnabled(request, ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS);
  }

  public boolean isJsBundlingEnabled() {
    return config.getBoolean(USE_JS_BUNDLES);
  }

  public ImmutableMap<String, Boolean> getAllFlags(Request request) {
    return ImmutableMap.of(
        ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS, allowCiviformAdminAccessPrograms(request),
        APPLICATION_STATUS_TRACKING_ENABLED, isStatusTrackingEnabled(request),
        PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED, isProgramEligibilityConditionsEnabled(request));
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
