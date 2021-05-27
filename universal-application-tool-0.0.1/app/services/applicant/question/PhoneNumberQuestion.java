package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;
import services.question.types.PhoneNumberQuestionDefinition;

public class PhoneNumberQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> textValue;

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

    PhoneNumberQuestionDefinition definition = getQuestionDefinition();
    int textLength = getTextValue().map(s -> s.length()).orElse(0);
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getTextPath());
  }

  public Optional<String> getTextValue() {
    if (textValue != null) {
      return textValue;
    }
    textValue = applicantQuestion.getApplicantData().readString(getTextPath());
    return textValue;
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

  public Path getTextPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.TEXT);
  }

  @Override
  public String getAnswerString() {
    return getTextValue().orElse("-");
  }
}
