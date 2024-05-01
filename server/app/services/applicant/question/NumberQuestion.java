package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.NumberQuestionDefinition;

/**
 * Represents a number question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class NumberQuestion extends Question {

  private Optional<Long> numberValue;

  NumberQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getNumberPath());
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of(getNumberPath(), validateNumber());
  }

  private ImmutableSet<ValidationErrorMessage> validateNumber() {
    Optional<Long> numberValue = getNumberValue();
    // When staging updates, the attempt to update ApplicantData would have failed to
    // convert to a number and been noted as a failed update. We check for that here.
    if (applicantQuestion.getApplicantData().updateDidFailAt(getNumberPath())
        || (numberValue.isPresent() && numberValue.get() < 0)) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_NON_INTEGER));
    }

    NumberQuestionDefinition questionDefinition = getQuestionDefinition();

    // If there is no minimum or maximum value configured, accept a blank answer.
    if (numberValue.isEmpty()
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
    return getNumberValue().map(Object::toString).orElse(getDefaultAnswerString());
  }
}
