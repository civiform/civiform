package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.MessageKey;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;
import services.question.types.PhoneNumberQuestionDefinition;

public class PhoneNumberQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> phoneNumberValue;

  public PhoneNumberQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  // Add simple back-end validation for phone number input
  // They must be in the form ("111-111-1111").
  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    int phoneNumberLength = getPhoneNumberValue().map(s -> s.length()).orElse(0);

    if (phoneNumberLength != 12 || !getPhoneNumberValue().get().matches("^([0-9]{3})-([0-9]{3})-([0-9]{4})$")) {
        errors.add(ValidationErrorMessage.create(MessageKey.PHONE_NUMBER_INVALID));
    }

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors() { return !getAllTypeSpecificErrors().isEmpty(); }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    return ImmutableSet.<ValidationErrorMessage>builder()
            .addAll(getLengthErrors())
            .addAll(getDigitErrors())
            .build();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getPhoneNumberPath());
  }

  // Checks if phone number has valid length (e.g. 10 digits)
  // Not exactly sure if we need a separate error creating function if we have getQuestionErrors()
  public ImmutableSet<ValidationErrorMessage> getLengthErrors() {
    // 10 digits plus two dashes (e.g. "111-111-1111")
    if (isPhoneNumberAnswered() && getPhoneNumberValue().get().length() != 12) {
      return ImmutableSet.of(
              ValidationErrorMessage.create(MessageKey.PHONE_NUMBER_INVALID));
    }

    return ImmutableSet.of();
  }

  // Checks if phone number has valid digit characters
  public ImmutableSet<ValidationErrorMessage> getDigitErrors() {
    if (isPhoneNumberAnswered() && !getPhoneNumberValue().get().matches("^([0-9]{3})-([0-9]{3})-([0-9]{4})$")) {
      return ImmutableSet.of(
              ValidationErrorMessage.create(MessageKey.PHONE_NUMBER_INVALID));
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

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.PHONENUMBER)) {
      throw new RuntimeException(
          String.format(
              "Question is not a PHONENUMBER question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public PhoneNumberQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (PhoneNumberQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getPhoneNumberPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.PHONE_NUMBER);
  }

  @Override
  public String getAnswerString() {
    return getPhoneNumberValue().orElse("-");
  }

  private boolean isPhoneNumberAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getPhoneNumberPath());
  }
}
