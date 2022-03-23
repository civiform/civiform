package views.style;

/** Non-styled classes that point to different components. */
public final class ReferenceClasses {

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // IMPORTANT: This file is parsed with regex when trimming unused styles
  //
  // See style/README.md
  //
  // Code constraints:
  //
  // - Though it is legal in Java to have a field declaration span multiple lines, doing so
  //   here will break the ability to parse those specific lines
  //
  // - Field variables can only have uppercase letters, numbers, and underscores. In other words,
  //   they should match /[0-9A-Z_]+/
  //   otherwise they will not show up in the final CSS style file without modifying the parse code
  //
  //   See RGX_KEY in tailwind.config.js
  //
  // - Field variable values can have lowercase letters, numbers, dash, period, and forward slash
  //   In other words, they need to match /[a-z0-9-/.]+/
  //   otherwise they will not show up in the final CSS style file without modifying the parse code
  //
  //   See RGX_VAL in tailwin.config.js
  //
  /////////////////////////////////////////////////////////////////////////////////////////////////

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Admin reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String ADMIN_APPLICATION_BLOCK_CARD = "cf-admin-application-block-card";
  public static final String ADMIN_APPLICATION_CARD = "cf-admin-application-card";
  public static final String ADMIN_LANGUAGE_LINK = "cf-admin-language-link";
  public static final String ADMIN_PROGRAM_CARD = "cf-admin-program-card";
  public static final String ADMIN_QUESTION_TABLE_ROW = "cf-admin-question-table-row";
  public static final String ADMIN_TI_GROUP_ROW = "cf-ti-row";
  public static final String ADMIN_VERSION_CARD = "cf-admin-version-card";
  public static final String QUESTION_CONFIG = "cf-question-config";
  public static final String EDIT_PREDICATE_BUTTON = "cf-edit-predicate";
  public static final String PREDICATE_DISPLAY = "cf-display-predicate";
  public static final String PREDICATE_ACTION = "cf-predicate-action";
  public static final String PREDICATE_SCALAR_SELECT = "cf-scalar-select";
  public static final String PREDICATE_OPERATOR_SELECT = "cf-operator-select";
  public static final String PREDICATE_OPTIONS = "cf-predicate-options";
  public static final String PREDICATE_VALUE_INPUT = "cf-predicate-value-input";
  public static final String PREDICATE_VALUE_COMMA_HELP_TEXT = "cf-predicate-value-comma-help-text";

  public static final String QUESTION_BANK_ELEMENT = "cf-question-bank-element";

  public static final String ADD_QUESTION_BUTTON = "cf-add-question-button";
  public static final String REMOVE_QUESTION_BUTTON = "cf-remove-question-button";

  public static final String DOWNLOAD_ALL_BUTTON = "cf-download-all-button";
  public static final String DOWNLOAD_BUTTON = "cf-download-button";
  public static final String VIEW_BUTTON = "cf-view-application-button";

  public static final String PROGRAM_ADMIN_REMOVE_BUTTON = "cf-program-admin-remove-button";
  public static final String PROGRAM_QUESTION = "cf-program-question";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Applicant reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String APPLICANT_QUESTION_HELP_TEXT = "cf-applicant-question-help-text";
  public static final String APPLICANT_QUESTION_TEXT = "cf-applicant-question-text";
  public static final String APPLICANT_SUMMARY_ROW = "cf-applicant-summary-row";
  public static final String APPLICATION_CARD = "cf-application-card";
  public static final String APPLICATION_CARD_DESCRIPTION = "cf-application-card-description";

  public static final String APPLY_BUTTON = "cf-apply-button";
  public static final String CONTINUE_BUTTON = "cf-continue-button";
  public static final String SUBMIT_BUTTON = "cf-submit-button";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Question reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String REQUIRED_QUESTION = "cf-question-required";

  public static final String ADDRESS_QUESTION = "cf-question-address";
  public static final String ADDRESS_CITY = "cf-address-city";
  public static final String ADDRESS_STREET_1 = "cf-address-street-1";
  public static final String ADDRESS_STREET_2 = "cf-address-street-2";
  public static final String ADDRESS_STATE = "cf-address-state";
  public static final String ADDRESS_ZIP = "cf-address-zip";

  public static final String MULTI_OPTION_QUESTION_OPTION = "cf-multi-option-question-option";
  public static final String MULTI_OPTION_INPUT = "cf-multi-option-input";

  public static final String CHECKBOX_QUESTION = "cf-question-checkbox";
  public static final String CURRENCY_QUESTION = "cf-question-currency";
  public static final String CURRENCY_VALUE = "cf-currency-value";
  public static final String DATE_QUESTION = "cf-question-date";
  public static final String EMAIL_QUESTION = "cf-question-email";

  public static final String ENUMERATOR_QUESTION = "cf-question-enumerator";
  public static final String ENUMERATOR_ERROR = "cf-enumerator-error";

  public static final String FILEUPLOAD_QUESTION = "cf-question-fileupload";
  public static final String FILEUPLOAD_ERROR = "cf-fileupload-error";

  public static final String NAME_QUESTION = "cf-question-name";
  public static final String NAME_FIRST = "cf-name-first";
  public static final String NAME_MIDDLE = "cf-name-middle";
  public static final String NAME_LAST = "cf-name-last";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Common reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String ACCORDION = "cf-accordion";
  public static final String ACCORDION_BUTTON = "cf-accordion-button";
  public static final String ACCORDION_CONTENT = "cf-accordion-content";
  public static final String ACCORDION_HEADER = "cf-accordion-header";

  public static final String QUESTION_TYPE = "cf-question-type";
  public static final String TOAST_MESSAGE = "cf-toast-data";
  public static final String ENTITY_NAME_INPUT = "cf-entity-name-input";
  public static final String ENUMERATOR_FIELD = "cf-enumerator-field";
  public static final String ENUMERATOR_EXISTING_DELETE_BUTTON = "cf-enumerator-delete-button";
  public static final String RADIO_DEFAULT = "cf-radio-default";
  public static final String RADIO_INPUT = "cf-radio-input";
  public static final String RADIO_OPTION = "cf-radio-option";
  public static final String MODAL = "cf-modal";
}
