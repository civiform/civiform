package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.NumberQuestionDefinition;
import services.question.QuestionType;

import java.util.Optional;

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
    if (!hasValue()) {
      return ImmutableSet.of();
    }

    NumberQuestionDefinition definition = getQuestionDefinition();
    long answer = getNumberValue().get();
    ImmutableSet.Builder<ValidationErrorMessage> errors =
            ImmutableSet.<ValidationErrorMessage>builder();

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

  public boolean hasValue() {
    return getNumberValue().isPresent();
  }

  public Optional<Long> getNumberValue() {
    if (numberValue != null) {
      return numberValue;
    }

    numberValue = applicantQuestion.applicantData.readLong(getNumberPath());

    return numberValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.NUMBER)) {
      throw new RuntimeException(
              String.format(
                      "Question is not a NUMBER question: %s (type: %s)",
                      applicantQuestion.questionDefinition.getPath(), applicantQuestion.questionDefinition.getQuestionType()));
    }
  }

  public NumberQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (NumberQuestionDefinition) applicantQuestion.questionDefinition;
  }

  public Path getNumberPath() {
    return getQuestionDefinition().getNumberPath();
  }
}
