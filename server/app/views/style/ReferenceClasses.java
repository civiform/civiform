package views.style;

/** Non-styled classes that point to different components. */
public final class ReferenceClasses {
  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Admin reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String ADMIN_APPLICATION_BLOCK_CARD = "cf-admin-application-block-card";
  public static final String ADMIN_APPLICATION_ROW = "cf-admin-application-row";
  public static final String ADMIN_LANGUAGE_LINK = "cf-admin-language-link";
  public static final String ADMIN_PROGRAM_CARD = "cf-admin-program-card";
  public static final String ADMIN_PROGRAM_CARD_TITLE = "cf-program-title";
  public static final String ADMIN_PROGRAM_CARD_DESCRIPTION = "cf-program-description";
  public static final String ADMIN_PROGRAM_STATUS_LIST = "cf-admin-program-status-list";
  public static final String ADMIN_PROGRAM_STATUS_ITEM = "cf-admin-program-status-item";
  public static final String ADMIN_PUBLISH_REFERENCES_PROGRAM =
      "cf-admin-publish-references-program";
  public static final String ADMIN_PUBLISH_REFERENCES_QUESTION =
      "cf-admin-publish-references-question";
  public static final String ADMIN_QUESTION_TABLE_ROW = "cf-admin-question-table-row";
  public static final String ADMIN_QUESTION_TITLE = "cf-question-title";
  public static final String ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS =
      "cf-admin-question-program-reference-counts";
  public static final String ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_USED =
      "cf-admin-question-program-reference-counts-used";
  public static final String ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_ADDED =
      "cf-admin-question-program-reference-counts-added";
  public static final String ADMIN_QUESTION_PROGRAM_REFERENCE_COUNTS_REMOVED =
      "cf-admin-question-program-reference-counts-removed";
  public static final String ADMIN_TI_GROUP_ROW = "cf-ti-row";
  public static final String QUESTION_CONFIG = "cf-question-config";
  public static final String EDIT_ELIGIBILITY_PREDICATE_LINK = "cf-edit-eligibility-predicate";
  public static final String EDIT_VISIBILITY_PREDICATE_LINK = "cf-edit-visibility-predicate";
  public static final String PREDICATE_DISPLAY = "cf-display-predicate";
  public static final String PREDICATE_ACTION = "cf-predicate-action";
  public static final String PREDICATE_QUESTION_NAME_FIELD = "cf-question-name-field";
  public static final String PREDICATE_SCALAR_SELECT = "cf-scalar-select";
  public static final String PREDICATE_OPERATOR_SELECT = "cf-operator-select";
  public static final String PREDICATE_OPTIONS = "cf-predicate-options";
  public static final String PREDICATE_VALUE_INPUT = "cf-predicate-value-input";
  public static final String PREDICATE_VALUE_SECOND_INPUT_CONTAINER =
      "cf-predicate-value-second-input-container";
  public static final String PREDICATE_VALUE_COMMA_HELP_TEXT = "cf-predicate-value-comma-help-text";
  public static final String PREDICATE_QUESTION_OPTIONS = "cf-predicate-question-options";

  public static final String QUESTION_BANK_ELEMENT = "cf-question-bank-element";
  public static final String QUESTION_BANK_HIDDEN = "cf-question-bank-hidden";
  public static final String ADD_QUESTION_BUTTON = "cf-add-question-button";
  public static final String REMOVE_QUESTION_BUTTON = "cf-remove-question-button";
  public static final String OPEN_QUESTION_BANK_BUTTON = "cf-open-question-bank-button";
  public static final String QUESTION_BANK_CONTAINER = "cf-question-bank-container";
  public static final String CLOSE_QUESTION_BANK_BUTTON = "cf-close-question-bank-button";
  public static final String QUESTION_BANK_GLASSPANE = "cf-question-bank-glasspane";
  public static final String QUESTION_BANK_PANEL = "cf-question-bank-panel";
  public static final String SORTABLE_QUESTIONS_CONTAINER = "cf-sortable-questions";

  public static final String DOWNLOAD_ALL_BUTTON = "cf-download-all-button";
  public static final String VIEW_BUTTON = "cf-view-application-button";

  public static final String PROGRAM_QUESTION = "cf-program-question";

  public static final String ADMIN_API_KEY_INDEX_ENTRY_NAME = "cf-api-key-name";

  public static final String WITH_DROPDOWN = "cf-with-dropdown";

  public static final String PROGRAM_ADMIN_STATUS_SELECTOR = "cf-program-admin-status-selector";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Applicant reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String TI_CLEAR_SEARCH = "cf-ti-clear-search";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Question reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String PHONE_NUMBER = "cf-phone-number";
  public static final String MULTI_OPTION_VALUE = "cf-multi-option-value";
  public static final String MULTI_OPTION_QUESTION_OPTION = "cf-multi-option-question-option";
  // Editable instance of a question option; not to be used for question previews.
  public static final String MULTI_OPTION_QUESTION_OPTION_EDITABLE =
      "cf-multi-option-question-option-editable";
  public static final String MULTI_OPTION_INPUT = "cf-multi-option-input";
  public static final String MULTI_OPTION_ADMIN_INPUT = "cf-multi-option-admin-input";

  // Keep these values in sync with app/assets/javascript/file_upload.ts.
  public static final String FILEUPLOAD_TOO_LARGE_ERROR_ID = "cf-fileupload-too-large-error";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Common reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////

  public static final String ACCORDION = "cf-accordion";
  public static final String ACCORDION_BUTTON = "cf-accordion-button";
  public static final String ACCORDION_CONTENT = "cf-accordion-content";
  public static final String ACCORDION_HEADER = "cf-accordion-header";

  public static final String TOAST_MESSAGE = "cf-toast-data";
  public static final String RADIO_INPUT = "cf-radio-input";
  public static final String RADIO_OPTION = "cf-radio-option";
  public static final String MODAL = "cf-modal";
  public static final String MODAL_CLOSE = "cf-modal-close";
  public static final String MODAL_DISPLAY_ON_LOAD = "cf-modal-display-on-load";
  // These classes mark fields that need dynamic replacement by browser tests when
  // taking a screenshot (e.g. dates, application IDs).
  public static final String BT_DATE = "cf-bt-date";
  public static final String BT_APPLICATION_ID = "cf-application-id";
  public static final String BT_EMAIL = "cf-bt-email";
  public static final String BT_API_KEY_ID = "cf-bt-api-key-id";
  public static final String BT_API_KEY_CREATED_BY = "cf-bt-api-key-created-by";

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Developer reference classes
  /////////////////////////////////////////////////////////////////////////////////////////////////
  public static final String DEV_ICONS = "cf-dev-icons";
}
