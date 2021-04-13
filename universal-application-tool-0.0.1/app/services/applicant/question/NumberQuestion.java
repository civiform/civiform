package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionType;

public class NumberQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<Long> numberValue;

  public NumberQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    NumberQuestionDefinition definition = getQuestionDefinition();
    long answer = getNumberValue().get();
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (definition.getMin().isPresent()) {
      long min = definition.getMin().getAsLong();
      if (answer < min) {
        errors.add(ValidationErrorMessage.numberTooSmallError(min));
      }
    }

    if (definition.getMax().isPresent()) {
      long max = definition.getMax().getAsLong();
      if (answer > max) {
        errors.add(ValidationErrorMessage.numberTooLargeError(max));
      }
    }

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    // There are no inherent requirements in a number question.
    return false;
  }

  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getNumberPath());
  }

  public Optional<Long> getNumberValue() {
    if (numberValue != null) {
      return numberValue;
    }

    numberValue = applicantQuestion.getApplicantData().readLong(getNumberPath());

    return numberValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.NUMBER)) {
      throw new RuntimeException(
          String.format(
              "Question is not a NUMBER question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getPath(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public NumberQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (NumberQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getNumberPath() {
    return getQuestionDefinition().getNumberPath();
  }
}
