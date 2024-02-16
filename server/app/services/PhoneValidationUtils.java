package services;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.Optional;

/** Utility Class for all phone validations. */
public final class PhoneValidationUtils {
  private static final String[] POTENTIAL_COUNTRY_CODES = new String[] {"US", "CA"};

  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  // Just a small container object to return multiple values *within* this class. Can be
  // replaced with Tuple when we get to Java 17+. If we need to expose this outside of
  // the class convert it to be an @AutoValue-based class.
  private static class PhoneValidationResult {
    public Optional<String> countryCode;
    public Optional<MessageKey> messageKey;

    private PhoneValidationResult(Optional<String> countryCode, Optional<MessageKey> messageKey) {
      this.countryCode = countryCode;
      this.messageKey = messageKey;
    }

    public static PhoneValidationResult create(String countryCode) {
      return new PhoneValidationResult(Optional.of(countryCode), Optional.empty());
    }

    public static PhoneValidationResult createError(MessageKey messageKey) {
      return new PhoneValidationResult(Optional.empty(), Optional.of(messageKey));
    }
  }

  /**
   * Gets an Optional MessageKey of Phone number and CountryCode values and runs validations on
   * them.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @param countryCode {@link Optional<String>}} of the Country Code
   * @return an {@link Optional<MessageKey>} representing the validation error that occurred if the
   *     phone number is invalid, or an Optional.empty() if the phone number is valid
   */
  public static Optional<MessageKey> validatePhoneNumber(
      Optional<String> phoneNumber, Optional<String> countryCode) {
    var result = validate(phoneNumber, countryCode);
    return result.messageKey;
  }

  /**
   * Gets an Optional string of Phone number and CountryCode values and runs validations on them.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @return an {@link Optional<String>} representing the country code if the phone number is valid,
   *     or an Optional.empty() if the phone number is invalid
   */
  public static Optional<String> determineCountryCode(Optional<String> phoneNumber) {
    for (String potentialCountryCode : POTENTIAL_COUNTRY_CODES) {
      var result = validate(phoneNumber, Optional.of(potentialCountryCode));

      if (result.messageKey.isEmpty() && result.countryCode.isPresent()) {
        return result.countryCode;
      }
    }

    return Optional.empty();
  }

  /**
   * Gets an {@link PhoneValidationResult} object with details on the validation result and valid
   * country code value.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @param countryCode {@link Optional<String>}} of the Country Code
   * @return an {@link PhoneValidationResult} representing the validation error that occurred if the
   *     phone number is invalid, or an Optional.empty() if the phone number is valid
   */
  private static PhoneValidationResult validate(
      Optional<String> phoneNumber, Optional<String> countryCode) {
    if (phoneNumber.isEmpty() || phoneNumber.get().isEmpty()) {
      return PhoneValidationResult.createError(MessageKey.PHONE_VALIDATION_NUMBER_REQUIRED);
    }

    if (countryCode.isEmpty() || countryCode.get().isEmpty()) {
      return PhoneValidationResult.createError(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
    }

    // removes space, '(',')' and '-' from the phone number
    phoneNumber = Optional.of(phoneNumber.get().replaceAll("[()-[\\s]]", ""));

    if (!phoneNumber.get().matches("[0-9]+")) {
      return PhoneValidationResult.createError(MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE);
    }

    if (phoneNumber.get().length() != 10) {
      return PhoneValidationResult.createError(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
    }

    try {
      Phonenumber.PhoneNumber phonenumber =
          PHONE_NUMBER_UTIL.parse(phoneNumber.get(), countryCode.get());

      if (!PHONE_NUMBER_UTIL.isValidNumber(phonenumber)) {
        return PhoneValidationResult.createError(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
      }

      if (!PHONE_NUMBER_UTIL.isValidNumberForRegion(phonenumber, countryCode.get())) {
        return PhoneValidationResult.createError(MessageKey.PHONE_VALIDATION_NUMBER_NOT_IN_COUNTRY);
      }
    } catch (NumberParseException e) {
      return PhoneValidationResult.createError(MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE);
    }

    return PhoneValidationResult.create(countryCode.get());
  }
}
