package services;

/** Contains keys into the {@code messages} files used for translation. */
public enum MessageKey {
  ADDRESS_LINE_2_LABEL("label.addressLine2"),
  ADDRESS_LINE_2_PLACEHOLDER("placeholder.line2"),
  ADD_REPEATER_ENTITY_BUTTON("button.addRepeaterEntity"),
  APPLICATION_SAVED_TOAST("toast.applicationSaved"),
  APPLY_BUTTON("button.apply"),
  CITY_LABEL("label.city"),
  CITY_PLACEHOLDER("placeholder.city"),
  CITY_REQUIRED("validation.cityRequired"),
  CIVIFORM_DESCRIPTION("content.description"),
  FIRST_NAME_LABEL("label.firstName"),
  FIRST_NAME_PLACEHOLDER("placeholder.firstName"),
  FIRST_NAME_REQUIRED("validation.firstNameRequired"),
  GET_BENEFITS("content.benefits"),
  INVALID_ZIP_CODE("validation.invalidZipcode"),
  LAST_NAME_LABEL("label.lastName"),
  LAST_NAME_PLACEHOLDER("placeholder.lastName"),
  LAST_NAME_REQUIRED("validation.lastNameRequired"),
  LOCALE_NOT_SUPPORTED_TOAST("toast.localeNotSupported"),
  LOGOUT_BUTTON("button.logout"),
  MIDDLE_NAME_LABEL("label.middleName"),
  MIDDLE_NAME_PLACEHOLDER("placeholder.middleName"),
  NEXT_BLOCK_BUTTON("button.nextBlock"),
  NO_CATEGORY("content.noCategory"),
  NO_PO_BOX("validation.noPoBox"),
  NUMBER_TOO_BIG("validation.numberTooBig"),
  NUMBER_TOO_SMALL("validation.numberTooSmall"),
  PROGRAM_COMPLETED_TOAST("toast.programCompleted"),
  PROGRAM_DETAILS("content.programDetails"),
  PROGRAM_PAGE_TITLE("title.programs"),
  SELECT_LANGUAGE("content.selectLanguage"),
  STATE_LABEL("label.state"),
  STATE_PLACEHOLDER("placeholder.state"),
  STATE_REQUIRED("validation.stateRequired"),
  STREET_LABEL("label.street"),
  STREET_PLACEHOLDER("placeholder.street"),
  STREET_REQUIRED("validation.streetRequired"),
  TEXT_TOO_LONG("validation.textTooLong"),
  TEXT_TOO_SHORT("validation.textTooShort"),
  TOO_FEW_SELECTIONS("validation.tooFewSelections"),
  TOO_MANY_SELECTIONS("validation.tooManySelections"),
  VIEW_APPLICATIONS_LINK("link.viewApplications"),
  ZIPCODE_LABEL("label.zipcode"),
  ZIPCODE_PLACEHOLDER("placeholder.zipcode"),
  ZIP_CODE_REQUIRED("validation.zipcodeRequired"),
  EMPTY_ENTITY_NAME("validation.entityNameRequired");

  private final String keyName;

  MessageKey(String keyName) {
    this.keyName = keyName;
  }

  public String getKeyName() {
    return this.keyName;
  }
}
