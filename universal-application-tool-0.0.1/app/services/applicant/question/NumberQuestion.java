package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import play.i18n.Messages;
import services.Path;
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
  public boolean hasQuestionErrors(Messages messages) {
    return !getQuestionErrors(messages).isEmpty();
  }

  @Override
  public ImmutableSet<String> getQuestionErrors(Messages messages) {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    NumberQuestionDefinition questionDefinition = getQuestionDefinition();

    // If there is no minimum or maximum value configured, accept a blank answer.
    if (getNumberValue().isEmpty()
        && questionDefinition.getMin().isEmpty()
        && questionDefinition.getMax().isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<String> errors = ImmutableSet.builder();

    if (questionDefinition.getMin().isPresent()) {
      long min = questionDefinition.getMin().getAsLong();
      // If value is empty, don't test against min.
      if (getNumberValue().isPresent() && getNumberValue().get() < min) {
        errors.add(messages.at("validation.numberTooSmall", min));
      }
    }

    if (questionDefinition.getMax().isPresent()) {
      long max = questionDefinition.getMax().getAsLong();
      // If value is empty, don't test against max.
      if (getNumberValue().isPresent() && getNumberValue().get() > max) {
        errors.add(messages.at("validation.numberTooLarge", max));
      }
    }

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors(Messages messages) {
    return !getAllTypeSpecificErrors(messages).isEmpty();
  }

  @Override
  public ImmutableSet<String> getAllTypeSpecificErrors(Messages messages) {
    // There are no inherent requirements in a number question.
    return ImmutableSet.of();
  }

  @Override
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
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public NumberQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (NumberQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getNumberPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.NUMBER);
  }
}
