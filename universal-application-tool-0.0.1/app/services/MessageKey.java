package services;

import play.i18n.Messages;

/** Contains keys into the {@code messages} files used for translation. */
public enum MessageKey {
  FIRST_NAME_REQUIRED("validation.firstNameRequired"),
  LAST_NAME_REQUIRED("validation.lastNameRequired"),
  TEXT_TOO_SHORT("validation.textTooShort"),
  TEXT_TOO_LONG("validation.textTooLong"),
  NUMBER_TOO_SMALL("validation.numberTooSmall"),
  NUMBER_TOO_BIG("validation.numberTooBig"),
  TOO_FEW_SELECTIONS("validation.tooFewSelections"),
  TOO_MANY_SELECTIONS("validation.tooManySelections"),
  STREET_REQUIRED("validation.streetRequired"),
  CITY_REQUIRED("validation.cityRequired"),
  STATE_REQUIRED("validation.stateRequired"),
  ZIP_CODE_REQUIRED("validation.zipcodeRequired"),
  INVALID_ZIP_CODE("validation.invalidZipcode"),
  NO_PO_BOX("validation.noPoBox");

  private final String keyName;

  MessageKey(String keyName) {
    this.keyName = keyName;
  }

  public String getKeyName() {
    return this.keyName;
  }

  public String getMessage(Messages messages) {
    return messages.at(this.keyName);
  }

  public String getMessage(Messages messages, Object... args) {
    return messages.at(this.keyName, args);
  }
}
