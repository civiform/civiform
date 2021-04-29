package forms;

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
}
