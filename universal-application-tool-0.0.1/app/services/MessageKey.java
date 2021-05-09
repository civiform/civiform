package services;

/** Contains keys into the {@code messages} files used for translation. */
public enum MessageKey {
  ADDRESS_LABEL_CITY("label.city"),
  ADDRESS_LABEL_LINE_2("label.addressLine2"),
  ADDRESS_LABEL_STATE("label.state"),
  ADDRESS_LABEL_STREET("label.street"),
  ADDRESS_LABEL_ZIPCODE("label.zipcode"),
  ADDRESS_PLACEHOLDER_CITY("placeholder.city"),
  ADDRESS_PLACEHOLDER_LINE_2("placeholder.line2"),
  ADDRESS_PLACEHOLDER_STATE("placeholder.state"),
  ADDRESS_PLACEHOLDER_STREET("placeholder.street"),
  ADDRESS_PLACEHOLDER_ZIPCODE("placeholder.zipcode"),
  ENUMERATOR_PLACEHOLDER_ENTITY_NAME("placeholder.entityName"),
  ADDRESS_VALIDATION_CITY_REQUIRED("validation.cityRequired"),
  ADDRESS_VALIDATION_INVALID_ZIPCODE("validation.invalidZipcode"),
  ADDRESS_VALIDATION_NO_PO_BOX("validation.noPoBox"),
  ADDRESS_VALIDATION_STATE_REQUIRED("validation.stateRequired"),
  ADDRESS_VALIDATION_STREET_REQUIRED("validation.streetRequired"),
  ADDRESS_VALIDATION_ZIPCODE_REQUIRED("validation.zipcodeRequired"),
  BUTTON_APPLY("button.apply"),
  BUTTON_LOGOUT("button.logout"),
  BUTTON_NEXT_BLOCK("button.nextBlock"),
  BUTTON_UNTRANSLATED_SUBMIT("button.untranslatedSubmit"),
  CONTENT_CIVIFORM_DESCRIPTION("content.description"),
  CONTENT_GET_BENEFITS("content.benefits"),
  CONTENT_NO_CATEGORY("content.noCategory"),
  CONTENT_PROGRAM_DETAILS("content.programDetails"),
  CONTENT_SELECT_LANGUAGE("content.selectLanguage"),
  ENUMERATOR_BUTTON_ADD_ENTITY("button.addEntity"),
  ENUMERATOR_BUTTON_ARIA_LABEL_DELETE_ENTITY("button.ariaLabel.deleteEntity"),
  ENUMERATOR_STRING_DEFAULT_ENTITY_TYPE("string.default.entityType"),
  ENUMERATOR_VALIDATION_ENTITY_REQUIRED("validation.entityNameRequired"),
  LINK_VIEW_APPLICATIONS("link.viewApplications"),
  MULTI_SELECT_VALIDATION_TOO_FEW("validation.tooFewSelections"),
  MULTI_SELECT_VALIDATION_TOO_MANY("validation.tooManySelections"),
  NAME_LABEL_FIRST("label.firstName"),
  NAME_LABEL_LAST("label.lastName"),
  NAME_LABEL_MIDDLE("label.middleName"),
  NAME_PLACEHOLDER_FIRST("placeholder.firstName"),
  NAME_PLACEHOLDER_LAST("placeholder.lastName"),
  NAME_PLACEHOLDER_MIDDLE("placeholder.middleName"),
  NAME_VALIDATION_FIRST_REQUIRED("validation.firstNameRequired"),
  NAME_VALIDATION_LAST_REQUIRED("validation.lastNameRequired"),
  NUMBER_VALIDATION_TOO_BIG("validation.numberTooBig"),
  NUMBER_VALIDATION_TOO_SMALL("validation.numberTooSmall"),
  TEXT_VALIDATION_TOO_LONG("validation.textTooLong"),
  TEXT_VALIDATION_TOO_SHORT("validation.textTooShort"),
  TITLE_PROGRAMS("title.programs"),
  TOAST_APPLICATION_SAVED("toast.applicationSaved"),
  TOAST_LOCALE_NOT_SUPPORTED("toast.localeNotSupported"),
  TOAST_PROGRAM_COMPLETED("toast.programCompleted");

  private final String keyName;

  MessageKey(String keyName) {
    this.keyName = keyName;
  }

  public String getKeyName() {
    return this.keyName;
  }
}
