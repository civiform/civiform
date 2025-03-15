package forms.translation;

import java.util.Locale;
import java.util.UUID;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

// NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING QuestionTranslationForm

/** Form for updating translation for questions. */
public class QuestionTranslationForm {

  private String questionText;
  private String questionHelpText;
  private UUID concurrencyToken;

  public QuestionTranslationForm() {
    this.questionText = "";
    this.questionHelpText = "";
    // If we don't get a token from the client, generate one so any updates fail.
    this.concurrencyToken = UUID.randomUUID();
  }

  public final String getQuestionText() {
    return questionText;
  }

  public final void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  public final String getQuestionHelpText() {
    return questionHelpText;
  }

  public final void setQuestionHelpText(String questionHelpText) {
    this.questionHelpText = questionHelpText;
  }

  public final void setConcurrencyToken(UUID concurrencyToken) {
    this.concurrencyToken = concurrencyToken;
  }

  public QuestionDefinitionBuilder builderWithUpdates(
      QuestionDefinition toUpdate, Locale updatedLocale) throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder(toUpdate);
    builder.updateQuestionText(updatedLocale, questionText);
    builder.setConcurrencyToken(concurrencyToken);
    // Help text is optional
    if (!questionHelpText.isBlank()) {
      builder.updateQuestionHelpText(updatedLocale, questionHelpText);
    }
    return builder;
  }
}
