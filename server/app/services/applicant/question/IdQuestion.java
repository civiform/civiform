package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.IdQuestionDefinition;

/**
 * Represents an id question in the context of a specific applicant. For example, this could be a
 * library account number or a bank account number.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class IdQuestion extends AbstractQuestion {

  private Optional<String> idValue;

  IdQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getIdPath());
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of(getIdPath(), validateId());
  }

  private ImmutableSet<ValidationErrorMessage> validateId() {
    IdQuestionDefinition definition = getQuestionDefinition();
    int idLength = getIdValue().map(String::length).orElse(0);
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (definition.getMinLength().isPresent()) {
      int minLength = definition.getMinLength().getAsInt();
      if (idLength < minLength) {
        errors.add(ValidationErrorMessage.create(MessageKey.ID_VALIDATION_TOO_SHORT, minLength));
      }
    }

    if (definition.getMaxLength().isPresent()) {
      int maxLength = definition.getMaxLength().getAsInt();
      if (idLength > maxLength) {
        errors.add(ValidationErrorMessage.create(MessageKey.ID_VALIDATION_TOO_LONG, maxLength));
      }
    }

    // Make sure the entered id is an int
    if (idLength != 0 && !getIdValue().get().matches("^[0-9]*$")) {
      errors.add(ValidationErrorMessage.create(MessageKey.ID_VALIDATION_NUMBER_REQUIRED));
    }

    return errors.build();
  }

  public Optional<String> getIdValue() {
    if (idValue != null) {
      return idValue;
    }
    idValue = applicantQuestion.getApplicantData().readString(getIdPath());
    return idValue;
  }

  public IdQuestionDefinition getQuestionDefinition() {
    return (IdQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getIdPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.ID);
  }

  @Override
  public String getAnswerString() {
    return getIdValue().orElse(getDefaultAnswerString());
  }
}
