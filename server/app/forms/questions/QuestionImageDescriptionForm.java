package forms.questions;

public final class QuestionImageDescriptionForm {
  public static final String QUESTION_IMAGE_DESCRIPTION = "questionImageDescription";

  private String questionImageDescription;

  public QuestionImageDescriptionForm() {}

  public QuestionImageDescriptionForm(String questionImageDescription) {
    this.questionImageDescription = questionImageDescription;
  }

  public String getQuestionImageDescription() {
    return questionImageDescription;
  }

  public void setQuestionImageDescription(String questionImageDescription) {
    this.questionImageDescription = questionImageDescription;
  }
}
