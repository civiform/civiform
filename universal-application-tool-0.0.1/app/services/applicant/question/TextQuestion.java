package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import play.i18n.Messages;
import services.Path;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;
import views.MessageKeys;

public class TextQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> textValue;

  public TextQuestion(ApplicantQuestion applicantQuestion) {
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

    TextQuestionDefinition definition = getQuestionDefinition();
    int textLength = getTextValue().map(s -> s.length()).orElse(0);
    ImmutableSet.Builder<String> errors = ImmutableSet.builder();

    if (definition.getMinLength().isPresent()) {
      int minLength = definition.getMinLength().getAsInt();
      if (textLength < minLength) {
        errors.add(messages.at(MessageKeys.TEXT_TOO_SHORT, minLength));
      }
    }

    if (definition.getMaxLength().isPresent()) {
      int maxLength = definition.getMaxLength().getAsInt();
      if (textLength > maxLength) {
        errors.add(messages.at(MessageKeys.TEXT_TOO_LONG, maxLength));
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
}
