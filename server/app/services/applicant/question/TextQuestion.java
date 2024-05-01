package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.TextQuestionDefinition;

/**
 * Represents a text question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class TextQuestion extends Question {

  private Optional<String> textValue;

  TextQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getTextPath());
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of(getTextPath(), validateText());
  }

  private ImmutableSet<ValidationErrorMessage> validateText() {
    TextQuestionDefinition definition = getQuestionDefinition();
    int textLength = getTextValue().map(String::length).orElse(0);
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (definition.getMinLength().isPresent()) {
      int minLength = definition.getMinLength().getAsInt();
      if (textLength < minLength) {
        errors.add(ValidationErrorMessage.create(MessageKey.TEXT_VALIDATION_TOO_SHORT, minLength));
      }
    }

    if (definition.getMaxLength().isPresent()) {
      int maxLength = definition.getMaxLength().getAsInt();
      if (textLength > maxLength) {
        errors.add(ValidationErrorMessage.create(MessageKey.TEXT_VALIDATION_TOO_LONG, maxLength));
      }
    }

    return errors.build();
  }

  public Optional<String> getTextValue() {
    if (textValue != null) {
      return textValue;
    }
    textValue = applicantQuestion.getApplicantData().readString(getTextPath());
    return textValue;
  }

  public TextQuestionDefinition getQuestionDefinition() {
    return (TextQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getTextPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.TEXT);
  }

  @Override
  public String getAnswerString() {
    return getTextValue().orElse(getDefaultAnswerString());
  }
}
