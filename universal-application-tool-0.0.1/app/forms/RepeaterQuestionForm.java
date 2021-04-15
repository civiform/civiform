package forms;

import services.question.types.QuestionType;
import services.question.types.RepeaterQuestionDefinition;

public class RepeaterQuestionForm extends QuestionForm {
  public RepeaterQuestionForm() {
    super();
  }

  public RepeaterQuestionForm(RepeaterQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.REPEATER;
  }
}
