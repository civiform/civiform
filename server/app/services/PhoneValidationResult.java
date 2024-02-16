package services;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * {@link PhoneValidationResult} contains the phone number, country code if match found, and,
 * validation message in the event validation fails
 */
@AutoValue
public abstract class PhoneValidationResult {
  public static Builder builder() {
    return new AutoValue_PhoneValidationResult.Builder();
  }

  public abstract Optional<String> getPhoneNumber();

  public abstract Optional<String> getCountryCode();

  public abstract Optional<MessageKey> getMessageKey();

  /**
   * Convenience method to create a {@link PhoneValidationResult} object when a validation checks
   * pass and a country code is found.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @param countryCode {@link Optional<String>}} of the Country Code in <a
   *     href="https://www.javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/8.4.1/com/google/i18n/phonenumbers/PhoneNumberUtil.html">CLDR
   *     format</a>
   * @return a {@link PhoneValidationResult} object when a validation checks pass
   */
  public static PhoneValidationResult create(
      Optional<String> phoneNumber, Optional<String> countryCode) {
    return PhoneValidationResult.builder()
        .setPhoneNumber(phoneNumber)
        .setCountryCode(countryCode)
        .build();
  }

  /**
   * Convenience method to create a {@link PhoneValidationResult} object when a validation check
   * fails.
   *
   * @param phoneNumber {@link Optional<String>}} of the Phone Number
   * @param messageKey {@link MessageKey} pertaining to the failed validation check
   * @return a {@link PhoneValidationResult} object when a validation check fails
   */
  public static PhoneValidationResult createError(
      Optional<String> phoneNumber, MessageKey messageKey) {
    return PhoneValidationResult.builder()
        .setPhoneNumber(phoneNumber)
        .setMessageKey(Optional.of(messageKey))
        .build();
  }

  /** Returns true if there are no validation errors and a country code exists */
  public boolean isValid() {
    return getMessageKey().isEmpty() && getCountryCode().isPresent();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setPhoneNumber(Optional<String> phoneNumber);

    public abstract Builder setCountryCode(Optional<String> countryCode);

    public abstract Builder setMessageKey(Optional<MessageKey> messageKey);

    public abstract PhoneValidationResult build();
  }
}
