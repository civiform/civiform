package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents a number question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class NumberQuestion extends QuestionImpl {

  private Optional<Long> numberValue;

  public NumberQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.NUMBER);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getNumberPath());
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
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

    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (questionDefinition.getMin().isPresent()) {
      long min = questionDefinition.getMin().getAsLong();
      // If value is empty, don't test against min.
      if (getNumberValue().isPresent() && getNumberValue().get() < min) {
        errors.add(ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_TOO_SMALL, min));
      }
    }

    if (questionDefinition.getMax().isPresent()) {
      long max = questionDefinition.getMax().getAsLong();
      // If value is empty, don't test against max.
      if (getNumberValue().isPresent() && getNumberValue().get() > max) {
        errors.add(ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_TOO_BIG, max));
      }
    }

    return errors.build();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a number question.
    return ImmutableSet.of();
  }

  public Optional<Long> getNumberValue() {
    if (numberValue != null) {
      return numberValue;
    }

    numberValue = applicantQuestion.getApplicantData().readLong(getNumberPath());

    return numberValue;
  }

  public NumberQuestionDefinition getQuestionDefinition() {
    return (NumberQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getNumberPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.NUMBER);
  }

  @Override
  public String getAnswerString() {
    return getNumberValue().map(Object::toString).orElse("-");
  }
}
