package services;

import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.Optional;

/** Utility Class for all phone validations. */
public final class PhoneValidationUtils {
  private static final ImmutableList<String> POTENTIAL_COUNTRY_CODES = ImmutableList.of("US", "CA");

  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  /**
   * Validates a phone number and country code.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @param countryCode {@link Optional<String>}} of the Country Code in <a
   *     href="https://www.javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/8.4.1/com/google/i18n/phonenumbers/PhoneNumberUtil.html">CLDR
   *     format</a>
   * @return an {@link Optional<MessageKey>} representing the validation error that occurred if the
   *     phone number is invalid, or an Optional.empty() if the phone number is valid
   */
  public static Optional<MessageKey> validatePhoneNumber(
      Optional<String> phoneNumber, Optional<String> countryCode) {
    PhoneValidationResult result = validate(phoneNumber, countryCode);
    return result.getMessageKey();
  }

  /**
   * Checks the supplied phone number with supported country codes to determine which, if any,
   * country code is valid.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @return an {@link Optional<String>} representing the <a
   *     href="https://www.javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/8.4.1/com/google/i18n/phonenumbers/PhoneNumberUtil.html">CLDR
   *     format</a> Country Code if the phone number is valid, or an Optional.empty() if the phone
   *     number is invalid
   */
  public static PhoneValidationResult determineCountryCode(Optional<String> phoneNumber) {
    PhoneValidationResult result = PhoneValidationResult.builder().build();

    for (String potentialCountryCode : POTENTIAL_COUNTRY_CODES) {
      result = validate(phoneNumber, Optional.of(potentialCountryCode));

      // Return early when we hit a valid country code.
      if (result.isValid()) {
        return result;
      }
    }

    return result;
  }

  /**
   * Validates a phone number and country code.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @param countryCode {@link Optional<String>}} of the Country Code in <a
   *     href="https://www.javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/8.4.1/com/google/i18n/phonenumbers/PhoneNumberUtil.html">CLDR
   *     format</a>
   * @return a {@link PhoneValidationResult} containing the phone number, country code, and possible
   *     validation error
   */
  private static PhoneValidationResult validate(
      Optional<String> phoneNumber, Optional<String> countryCode) {

    if (phoneNumber.isEmpty() || phoneNumber.get().isEmpty()) {
      return PhoneValidationResult.createError(
          phoneNumber, MessageKey.PHONE_VALIDATION_NUMBER_REQUIRED);
    }

    if (countryCode.isEmpty() || countryCode.get().isEmpty()) {
      return PhoneValidationResult.createError(
          phoneNumber, MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
    }

    // removes space, '(',')' and '-' from the phone number
    String cleanedPhoneNumber = phoneNumber.get().replaceAll("[()-[\\s]]", "");

    if (!cleanedPhoneNumber.matches("[0-9]+")) {
      return PhoneValidationResult.createError(
          phoneNumber, MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE);
    }

    if (cleanedPhoneNumber.length() != 10) {
      return PhoneValidationResult.createError(
          phoneNumber, MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
    }

    try {
      Phonenumber.PhoneNumber phonenumber =
          PHONE_NUMBER_UTIL.parse(cleanedPhoneNumber, countryCode.get());

      if (!PHONE_NUMBER_UTIL.isValidNumber(phonenumber)) {
        return PhoneValidationResult.createError(
            phoneNumber, MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
      }

      if (!PHONE_NUMBER_UTIL.isValidNumberForRegion(phonenumber, countryCode.get())) {
        return PhoneValidationResult.createError(
            phoneNumber, MessageKey.PHONE_VALIDATION_NUMBER_NOT_IN_COUNTRY);
      }
    } catch (NumberParseException e) {
      return PhoneValidationResult.createError(
          phoneNumber, MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE);
    }

    return PhoneValidationResult.create(phoneNumber, countryCode);
  }

  /**
   * Checks the supplied phone number with supported country codes to determine which, if any,
   * country code is valid.
   *
   * <p>Really deep dive into E.164 can be found on the <a
   * href="https://www.itu.int/rec/t-rec-e.164/en">ITU site</a>, but a short overview can be seen on
   * <a href="https://www.twilio.com/docs/glossary/what-e164">Twilio's site</a>.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @return an {@link Optional<String>} representing the <a
   *     href="https://www.javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/8.4.1/com/google/i18n/phonenumbers/PhoneNumberUtil.html">CLDR
   *     format</a> Country Code if the phone number is valid, or an Optional.empty() if the phone
   *     number is invalid
   */
  public static PhoneValidationResult determineCountryCodeForE164PhoneNumber(
      Optional<String> phoneNumber) {
    PhoneValidationResult result = PhoneValidationResult.builder().build();

    for (String potentialCountryCode : POTENTIAL_COUNTRY_CODES) {
      result = validateForE164PhoneNumber(phoneNumber, Optional.of(potentialCountryCode));

      // Return early when we hit a valid country code.
      if (result.isValid()) {
        return result;
      }
    }

    return result;
  }

  /**
   * Validates a phone number and country code.
   *
   * <p>Really deep dive into E.164 can be found on the <a
   * href="https://www.itu.int/rec/t-rec-e.164/en">ITU site</a>, but a short overview can be seen on
   * <a href="https://www.twilio.com/docs/glossary/what-e164">Twilio's site</a>.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @param countryCode {@link Optional<String>}} of the Country Code in <a
   *     href="https://www.javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/8.4.1/com/google/i18n/phonenumbers/PhoneNumberUtil.html">CLDR
   *     format</a>
   * @return a {@link PhoneValidationResult} containing the phone number, country code, and possible
   *     validation error
   */
  private static PhoneValidationResult validateForE164PhoneNumber(
      Optional<String> phoneNumber, Optional<String> countryCode) {
    try {
      boolean isE164NumberFormat = phoneNumber.orElse("").startsWith("+");

      Phonenumber.PhoneNumber parsedPhoneNumber =
          PHONE_NUMBER_UTIL.parse(
              phoneNumber.orElse(""), isE164NumberFormat ? null : countryCode.orElse(""));

      if (isE164NumberFormat) {
        countryCode = Optional.of(PHONE_NUMBER_UTIL.getRegionCodeForNumber(parsedPhoneNumber));
      }

      if (!PHONE_NUMBER_UTIL.isValidNumber(parsedPhoneNumber)
          || !PHONE_NUMBER_UTIL.isValidNumberForRegion(parsedPhoneNumber, countryCode.orElse(""))) {
        return PhoneValidationResult.createError(
            phoneNumber, MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
      }

      if (!POTENTIAL_COUNTRY_CODES.contains(countryCode.orElse(""))) {
        return PhoneValidationResult.createError(
            phoneNumber, MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
      }

      return PhoneValidationResult.create(
          Optional.of(Long.toString(parsedPhoneNumber.getNationalNumber())), countryCode);
    } catch (NumberParseException e) {
      return PhoneValidationResult.createError(
          phoneNumber, MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER);
    }
  }
}
