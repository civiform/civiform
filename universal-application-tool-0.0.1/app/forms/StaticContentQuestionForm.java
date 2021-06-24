package forms;

import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

public class StaticContentQuestionForm extends QuestionForm {

  public StaticContentQuestionForm() {
    super();
  }

  public StaticContentQuestionForm(QuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.STATIC;
  }
}
