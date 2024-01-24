package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.PhoneValidationUtils;
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
  
  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();


  PhoneQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getPhoneNumberPath(), getCountryCodePath());
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // TODO: Implement admin-defined validation.
    return ImmutableMap.of(
        getPhoneNumberPath(), validatePhoneNumber(),
        getCountryCodePath(), validateCountryCode());
  }

  private ImmutableSet<ValidationErrorMessage> validatePhoneNumber() {
    Optional<MessageKey> validationErrors =
        PhoneValidationUtils.validatePhoneNumber(getPhoneNumberValue(), getCountryCodeValue());
    if (validationErrors.isPresent()) {
      return ImmutableSet.of(ValidationErrorMessage.create(validationErrors.get()));
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

  /**
   * Returns formatted phone number or defaults to "-" if formatting fails. An example of formatted
   * number is :+1 206-648-2489
   */
  @Override
  public String getAnswerString() {
    try {
      Phonenumber.PhoneNumber phoneNumber =
          PHONE_NUMBER_UTIL.parse(
              getPhoneNumberValue().orElse(""), getCountryCodeValue().orElse(""));
      return PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    } catch (NumberParseException e) {
      return "-";
    }
  }
}
