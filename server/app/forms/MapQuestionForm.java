package forms;

import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

public class MapQuestionForm extends QuestionForm {
  public MapQuestionForm() {
    super();
  }

  public MapQuestionForm(QuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.MAP;
  }
}
