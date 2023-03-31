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
 */
public enum FeatureFlag {
  // Main control for any feature flags working.
  FEATURE_FLAG_OVERRIDES_ENABLED,

  // Long lived feature flags.
  ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS,
  SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE,

  // Launch Flags, these will eventually be removed.
  PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED,
  PROGRAM_READ_ONLY_VIEW_ENABLED,

  // Address correction and verifcation flags
  ESRI_ADDRESS_CORRECTION_ENABLED,
  ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED,

  // Common Intake Form flags.
  INTAKE_FORM_ENABLED,
  NONGATED_ELIGIBILITY_ENABLED,

  // Phone number question type.
  PHONE_QUESTION_TYPE_ENABLED,

  // New login form
  NEW_LOGIN_FORM_ENABLED;

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
