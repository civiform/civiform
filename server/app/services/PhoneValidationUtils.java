package services;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.Optional;

/** Utility Class for all phone validations. */
public final class PhoneValidationUtils {

  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  /**
   * Gets an Optional string of Phone number and CountryCode values and runs validations on them.
   *
   * @return an Optional {@link MessageKey} representing the validation error that occurred if the
   *     phone number is invalid, or an Optional.empty() if the phone number is valid
   */
  public static Optional<MessageKey> validatePhoneNumber(
      Optional<String> phoneNumber, Optional<String> countryCode) {
    if (phoneNumber.isEmpty()) {
      return Optional.of(MessageKey.PHONE_VALIDATION_NUMBER_REQUIRED);
    }

    if (countryCode.isEmpty()) {
      return Optional.of(MessageKey.PHONE_VALIDATION_COUNTRY_CODE_REQUIRED);
    }
    // removes space, '(',')' and '-' from the phone number
    phoneNumber = Optional.of(phoneNumber.get().replaceAll("[()-[\\s]]", ""));

    if (!phoneNumber.get().matches("[0-9]+")) {
      return Optional.of(MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE);
    }
    if (phoneNumber.get().length() != 10) {
      return Optional.of(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
    }
    try {
      Phonenumber.PhoneNumber phonenumber =
          PHONE_NUMBER_UTIL.parse(phoneNumber.get(), countryCode.get());
      if (!PHONE_NUMBER_UTIL.isValidNumber(phonenumber)) {
        return Optional.of(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
      }
      if (!PHONE_NUMBER_UTIL.isValidNumberForRegion(phonenumber, countryCode.get())) {
        return Optional.of(MessageKey.PHONE_VALIDATION_NUMBER_NOT_IN_COUNTRY);
      }
    } catch (NumberParseException e) {
      return Optional.of(MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE);
    }
    return Optional.empty();
  }
}
