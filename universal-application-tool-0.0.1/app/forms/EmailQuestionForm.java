package forms;

import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

public class EmailQuestionForm extends QuestionForm {

  public EmailQuestionForm() {
    super();
  }

  public EmailQuestionForm(QuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.EMAIL;
  }
}
