package forms.translation;

import java.util.Locale;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class QuestionTranslationForm {

  private String questionText;
  private String questionHelpText;

  public QuestionTranslationForm() {
    this.questionText = "";
    this.questionHelpText = "";
  }

  public String getQuestionText() {
    return questionText;
  }

  public void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  public String getQuestionHelpText() {
    return questionHelpText;
  }

  public void setQuestionHelpText(String questionHelpText) {
    this.questionHelpText = questionHelpText;
  }

  public QuestionDefinitionBuilder builderWithUpdates(
      QuestionDefinition toUpdate, Locale updatedLocale) throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder(toUpdate);
    builder.updateQuestionText(updatedLocale, questionText);
    builder.updateQuestionHelpText(updatedLocale, questionHelpText);
    return builder;
  }
}
