package services.applicant.question;

import com.google.common.collect.ImmutableList;
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

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    Optional<String> phoneNumber = getPhoneNumberValue();

    if (!phoneNumber.get().matches("^([0-9]{10})$")) {
        errors.add(ValidationErrorMessage.create(MessageKey.PHONE_NUMBER_INVALID));
    }

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors() { return !getAllTypeSpecificErrors().isEmpty(); }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    return ImmutableSet.<ValidationErrorMessage>builder().build();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getPhoneNumberPath());
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

  @Override
  public ImmutableList<Path> getAllPaths() { return ImmutableList.of(getPhoneNumberPath()); }
}
