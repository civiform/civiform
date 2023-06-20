package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.PhoneQuestionDefinition;

/**
 * Represents a phone question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class PhoneQuestion extends Question {

  private Optional<String> phoneNumberValue;
  private Optional<String> countryCodeValue;
  private static final Logger logger = LoggerFactory.getLogger(PhoneQuestion.class);

  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  PhoneQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getPhoneNumberPath(), getCountryCodePath());
  }

  @Override
  public ImmutableMap<Path, String> getJsonEntries() {
    Path path = getPhoneNumberPath().asApplicationPath();

    if (getPhoneNumberValue().isPresent() && getCountryCodeValue().isPresent()) {
      String formattedPhone =
          getFormattedPhoneNumber(getPhoneNumberValue().get(), getCountryCodeValue().get());
      return ImmutableMap.of(path, formattedPhone);
    } else {
      return ImmutableMap.of();
    }
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // TODO: Implement admin-defined validation.
    return ImmutableMap.of(
        getPhoneNumberPath(), validatePhoneNumber(),
        getCountryCodePath(), validateCountryCode());
  }

  private ImmutableSet<ValidationErrorMessage> validatePhoneNumber() {
    if (getPhoneNumberValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_NUMBER_REQUIRED));
    }
    if (getCountryCodeValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_COUNTRY_CODE_REQUIRED));
    }
    if (!getPhoneNumberValue().get().matches("[0-9]+")) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE));
    }
    try {
      Phonenumber.PhoneNumber phonenumber =
          PHONE_NUMBER_UTIL.parse(getPhoneNumberValue().get(), getCountryCodeValue().get());
      if (!PHONE_NUMBER_UTIL.isValidNumber(phonenumber)) {
        return ImmutableSet.of(
            ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER));
      }
      if (!PHONE_NUMBER_UTIL.isValidNumberForRegion(phonenumber, getCountryCodeValue().get())) {
        return ImmutableSet.of(
            ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_NUMBER_NOT_IN_COUNTRY));
      }
    } catch (NumberParseException e) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE));
    }

    return ImmutableSet.of();
  }

  private ImmutableSet<ValidationErrorMessage> validateCountryCode() {
    if (getCountryCodeValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_COUNTRY_CODE_REQUIRED));
    }

    return ImmutableSet.of();
  }

  public Optional<String> getPhoneNumberValue() {
    if (phoneNumberValue != null) {
      return phoneNumberValue;
    }
    phoneNumberValue = applicantQuestion.getApplicantData().readString(getPhoneNumberPath());
    return phoneNumberValue;
  }

  public Optional<String> getCountryCodeValue() {
    if (countryCodeValue != null) {
      return countryCodeValue;
    }

    countryCodeValue = applicantQuestion.getApplicantData().readString(getCountryCodePath());

    return countryCodeValue;
  }

  public PhoneQuestionDefinition getQuestionDefinition() {
    return (PhoneQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getPhoneNumberPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.PHONE_NUMBER);
  }

  public Path getCountryCodePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.COUNTRY_CODE);
  }

  @Override
  public String getAnswerString() {
    try {
      Phonenumber.PhoneNumber phoneNumber =
          PHONE_NUMBER_UTIL.parse(
              getPhoneNumberValue().orElse(""), getCountryCodeValue().orElse(""));
      return PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    } catch (NumberParseException e) {
      logger.error(
          "Unable to retrieve or parse phone number "
              + getPhoneNumberValue().orElse("")
              + "for country_code "
              + getCountryCodeValue().orElse(""));
    }
    return "-";
  }

  /**
   * This method accepts a phoneNumber as String and the countryCode which is iso alpha 2 format as
   * a String. It formats the phone number per E164 format. For a sample input of
   * phoneNumberValue="2123456789" with countryCode="US", the output will be +12123456789
   */
  private String getFormattedPhoneNumber(String phoneNumberValue, String countryCode) {
    try {
      Phonenumber.PhoneNumber phoneNumber = PHONE_NUMBER_UTIL.parse(phoneNumberValue, countryCode);
      return PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    } catch (NumberParseException e) {
      throw new RuntimeException(e);
    }
  }
}
