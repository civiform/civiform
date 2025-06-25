package forms;

import services.question.types.MapQuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a map question. */
public class MapQuestionForm extends QuestionForm {

  public MapQuestionForm() {
    super();
  }

  public MapQuestionForm(MapQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.MAP;
  }
}
