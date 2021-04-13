package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;

public class TextQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> textValue;

  public TextQuestion(ApplicantQuestion applicantQuestion) {
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

    TextQuestionDefinition definition = getQuestionDefinition();
    int textLength = getTextValue().map(s -> s.length()).orElse(0);
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (definition.getMinLength().isPresent()) {
      int minLength = definition.getMinLength().getAsInt();
      if (textLength < minLength) {
        errors.add(ValidationErrorMessage.textTooShortError(minLength));
      }
    }

    if (definition.getMaxLength().isPresent()) {
      int maxLength = definition.getMaxLength().getAsInt();
      if (textLength > maxLength) {
        errors.add(ValidationErrorMessage.textTooLongError(maxLength));
      }
    }

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    // There are no inherent requirements in a text question.
    return false;
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
    if (!applicantQuestion.getType().equals(QuestionType.TEXT)) {
      throw new RuntimeException(
          String.format(
              "Question is not a TEXT question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getPath(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public TextQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (TextQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getTextPath() {
    return getQuestionDefinition().getTextPath();
  }
}
