package featureflags;

import com.google.common.base.CaseFormat;

public enum FeatureFlag {
  // Main control for any feature flags working.
  FEATURE_FLAG_OVERRIDES_ENABLED("feature_flag_overrides_enabled"),

  // Long lived feature flags.
  ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS("allow_civiform_admin_access_programs"),
  ADMIN_REPORTING_UI_ENABLED("admin_reporting_ui_enabled"),
  SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE("show_civiform_image_tag_on_landing_page"),

  // Launch Flags, these will eventually be removed.
  PROGRAM_ELIGIBILITY_CONDITIONS_ENABLED("program_eligibility_conditions_enabled"),
  PROGRAM_READ_ONLY_VIEW_ENABLED("program_read_only_view_enabled"),

  // Address correction and verifcation flags
  ESRI_ADDRESS_CORRECTION_ENABLED("esri_address_correction_enabled"),
  ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED("esri_address_service_area_validation_enabled"),

  // Common Intake Form flags.
  INTAKE_FORM_ENABLED("intake_form_enabled"),
  NONGATED_ELIGIBILITY_ENABLED("nongated_eligibility_enabled"),

  // Phone number question type.
  PHONE_QUESTION_TYPE_ENABLED("phone_question_type_enabled");

  private final String symbol;

  FeatureFlag(String symbol) {
    if (!symbolValid(symbol)) {
      throw new RuntimeException(
          "Symbol must be the lower_underscore version of its UPPER_UNDERSCORE enum name.");
    }

    this.symbol = symbol;
  }

  private boolean symbolValid(String symbolParam) {
    String upperUnderscoreName =
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, name());
    return symbolParam.equals(upperUnderscoreName);
  }

  public String getSymbol() {
    return symbol;
  }

  public static FeatureFlag getBySymbol(String symbol) {
    for (FeatureFlag flag : values()) {
      if (symbol.equals(flag.getSymbol())) {
        return flag;
      }
    }
    throw new RuntimeException("No flag found");
  }
}
