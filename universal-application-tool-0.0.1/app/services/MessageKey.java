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
  ADDRESS_VALIDATION_CITY_REQUIRED("validation.cityRequired"),
  ADDRESS_VALIDATION_INVALID_ZIPCODE("validation.invalidZipcode"),
  ADDRESS_VALIDATION_NO_PO_BOX("validation.noPoBox"),
  ADDRESS_VALIDATION_STATE_REQUIRED("validation.stateRequired"),
  ADDRESS_VALIDATION_STREET_REQUIRED("validation.streetRequired"),
  ADDRESS_VALIDATION_ZIPCODE_REQUIRED("validation.zipcodeRequired"),
  BUTTON_APPLY("button.apply"),
  BUTTON_CONTINUE("button.continue"),
  BUTTON_LOGOUT("button.logout"),
  BUTTON_NEXT_BLOCK("button.nextBlock"),
  BUTTON_REVIEW("button.review"),
  BUTTON_SUBMIT("button.submit"),
  BUTTON_UNTRANSLATED_SUBMIT("button.untranslatedSubmit"),
  CONTENT_CIVIFORM_DESCRIPTION_1("content.description1"),
  CONTENT_CIVIFORM_DESCRIPTION_2("content.description2"),
  CONTENT_CONFIRMED("content.confirmed"),
  CONTENT_GET_BENEFITS("content.benefits"),
  CONTENT_NO_CATEGORY("content.noCategory"),
  CONTENT_PLEASE_CREATE_ACCOUNT("content.pleaseCreateAccount"),
  CONTENT_SELECT_LANGUAGE("label.selectLanguage"),
  ENUMERATOR_BUTTON_ADD_ENTITY("button.addEntity"),
  ENUMERATOR_BUTTON_REMOVE_ENTITY("button.removeEntity"),
  ENUMERATOR_PLACEHOLDER_ENTITY_NAME("placeholder.entityName"),
  ENUMERATOR_VALIDATION_ENTITY_REQUIRED("validation.entityNameRequired"),
  ENUMERATOR_VALIDATION_DUPLICATE_ENTITY_NAME("validation.duplicateEntityName"),
  FOOTER_SUPPORT_LINK_DESCRIPTION("footer.supportLinkDescription"),
  LINK_ALL_DONE("link.allDone"),
  LINK_APPLY_TO_ANOTHER_PROGRAM("link.applyToAnotherProgram"),
  LINK_BEGIN("link.begin"),
  LINK_CREATE_ACCOUNT_OR_SIGN_IN("link.createAccountOrSignIn"),
  LINK_EDIT("link.edit"),
  LINK_PROGRAM_DETAILS("link.programDetails"),
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
  TITLE_APPLICATION_CONFIRMATION("title.applicationConfirmation"),
  TITLE_CREATE_AN_ACCOUNT("title.createAnAccount"),
  TITLE_PROGRAMS("title.programs"),
  TITLE_PROGRAM_PREVIEW("title.programPreview"),
  TITLE_PROGRAM_REVIEW("title.programReview"),
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
