package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;

/**
 * Represents a text question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class TextQuestion implements Question {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> textValue;

  public TextQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getTextPath());
  }

  @Override
  public boolean hasConditionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
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

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a text question.
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
    if (!applicantQuestion.getType().equals(QuestionType.TEXT)) {
      throw new RuntimeException(
          String.format(
              "Question is not a TEXT question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public TextQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (TextQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getTextPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.TEXT);
  }

  @Override
  public String getAnswerString() {
    return getTextValue().orElse("-");
  }
}
