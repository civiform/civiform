package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionType;


/**
 * Represents a phone question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class PhoneQuestion extends Question {

  private Optional<String> phoneNumberValue;
  private Optional<String> countryCodeValue;

  PhoneQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.NAME);
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
    if (getPhoneNumberValue().isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_NUMBER_REQUIRED));
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
    return getPhoneNumberValue().orElse("") + getCountryCodeValue().orElse("");
  }
}
