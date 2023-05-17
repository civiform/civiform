package featureflags;

/**
 * An enum to represent feature flags used in CiviForm.
 *
 * <p>Never use the name() method outside this class.
 *
 * <p>The built-in Enum name() method returns the exact enum name the enum is defined as in
 * UPPER_CAMEL_CASE. Therefore, it is imperative that we use the overriden toString() method to
 * ensure that the lower_camel_case version of the flag is returned.
 *
 * <p>For example, a FeatureFlag with value MY_FLAG will appear in configuration as "my_flag".
 *
 * <p>See https://docs.civiform.us/contributor-guide/developer-guide/feature-flags.
 */
public enum FeatureFlag {
  // Main control for any feature flags working.
  FEATURE_FLAG_OVERRIDES_ENABLED,

  // Long lived feature flags.
  ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS,
  SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE,

  // Address correction and verification flags
  ESRI_ADDRESS_CORRECTION_ENABLED,
  ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED,

  // Common Intake Form flags.
  INTAKE_FORM_ENABLED,
  NONGATED_ELIGIBILITY_ENABLED,

  // Phone number question type.
  PHONE_QUESTION_TYPE_ENABLED,

  // Whether to bypass the login and language screens and automatically and consider
  // a new user to be a guest until they log in.
  // TODO(#4705): remove this feature flag and make this behavior the default.
  BYPASS_LOGIN_LANGUAGE_SCREENS,

  // Single program publishing feature flag.
  PUBLISH_SINGLE_PROGRAM_ENABLED;

  /**
   * Returns a {@link FeatureFlag} for the given name. Matches based on the first matching flag,
   * case-insensitive. Therefore, it doesn't matter whether the parameter is UPPER_CAMEL_CASE or
   * lower_camel_case.
   */
  public static FeatureFlag getByName(String name) {
    for (FeatureFlag flag : values()) {
      if (name.equalsIgnoreCase(flag.name())) {
        return flag;
      }
    }
    throw new RuntimeException("No flag found");
  }

  /** Returns a configuration-friendly version of the flag value in lower_camel_case. */
  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
