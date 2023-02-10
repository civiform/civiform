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
  public static final String SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE =
      "show_civiform_image_tag_on_landing_page";

  // Launch Flags, these will eventually be removed.
  public static final String PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED =
      "program_eligibility_conditions_enabled";
  public static final String PROGRAM_READ_ONLY_VIEW_ENABLED = "program_read_only_view_enabled";
  public static final String PREDICATES_MULTIPLE_QUESTIONS_ENABLED =
      "predicates_multiple_questions_enabled";

  private final Config config;

  // Address correction and verifcation flags
  private static final String ESRI_ADDRESS_CORRECTION_ENABLED = "esri_address_correction_enabled";
  private static final String ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED =
      "esri_address_service_area_validation_enabled";

  // Common Intake Form flags.
  private static final String INTAKE_FORM_ENABLED = "intake_form_enabled";
  private static final String NONGATED_ELIGIBILITY_ENABLED = "nongated_eligibility_enabled";

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
   * If specifying multiple questions in a predicate is enabled.
   *
   * <p>Allows for overrides set in {@code request}.
   */
  public boolean isPredicatesMultipleQuestionsEnabled(Request request) {
    return getFlagEnabled(request, PREDICATES_MULTIPLE_QUESTIONS_ENABLED);
  }

  public boolean allowCiviformAdminAccessPrograms(Request request) {
    return getFlagEnabled(request, ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS);
  }

  /**
   * If the CiviForm image tag is show on the landing page.
   *
   * <p>Allows for overrides set in {@code request}.
   */
  public boolean showCiviformImageTagOnLandingPage(Request request) {
    return getFlagEnabled(request, SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE);
  }

  // If the UI can show a read only view of a program. Without this flag the
  // only way to view a program is to start editing it.
  public boolean isReadOnlyProgramViewEnabled() {
    return config.getBoolean(PROGRAM_READ_ONLY_VIEW_ENABLED);
  }

  public boolean isReadOnlyProgramViewEnabled(Request request) {
    return getFlagEnabled(request, PROGRAM_READ_ONLY_VIEW_ENABLED);
  }

  public boolean isEsriAddressCorrectionEnabled(Request request) {
    return getFlagEnabled(request, ESRI_ADDRESS_CORRECTION_ENABLED);
  }

  public boolean isEsriAddressServiceAreaValidationEnabled(Request request) {
    return getFlagEnabled(request, ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED);
  }

  public boolean isIntakeFormEnabled(Request request) {
    return getFlagEnabled(request, INTAKE_FORM_ENABLED);
  }

  public boolean isNongatedEligibilityEnabled(Request request) {
    return getFlagEnabled(request, NONGATED_ELIGIBILITY_ENABLED);
  }

  public ImmutableMap<String, Boolean> getAllFlags(Request request) {
    return ImmutableMap.of(
        ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS,
        allowCiviformAdminAccessPrograms(request),
        SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE,
        showCiviformImageTagOnLandingPage(request),
        PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED,
        isProgramEligibilityConditionsEnabled(request),
        PREDICATES_MULTIPLE_QUESTIONS_ENABLED,
        isPredicatesMultipleQuestionsEnabled(request),
        PROGRAM_READ_ONLY_VIEW_ENABLED,
        isReadOnlyProgramViewEnabled(request),
        ESRI_ADDRESS_CORRECTION_ENABLED,
        isEsriAddressCorrectionEnabled(request),
        ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED,
        isEsriAddressServiceAreaValidationEnabled(request),
        INTAKE_FORM_ENABLED,
        isIntakeFormEnabled(request),
        NONGATED_ELIGIBILITY_ENABLED,
        isNongatedEligibilityEnabled(request));
  }

  /**
   * Returns the current setting for {@code flag} from {@link Config} if present, allowing for an
   * overriden value from the session cookie.
   *
   * <p>Returns false if the value is not present.
   */
  private boolean getFlagEnabled(Request request, String flag) {
    Optional<Boolean> maybeConfigValue = getFlagEnabledFromConfig(flag);
    if (maybeConfigValue.isEmpty()) {
      return false;
    }
    Boolean configValue = maybeConfigValue.get();

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

  /** Returns the current setting for {@code flag} from {@link Config} if present. */
  public Optional<Boolean> getFlagEnabledFromConfig(String flag) {
    if (!config.hasPath(flag)) {
      logger.warn("Feature flag requested for unconfigured flag: {}", flag);
      return Optional.empty();
    }
    return Optional.of(config.getBoolean(flag));
  }
}
