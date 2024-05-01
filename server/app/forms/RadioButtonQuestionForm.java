package forms;

import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a radio button question. */
public class RadioButtonQuestionForm extends MultiOptionQuestionForm {

  public RadioButtonQuestionForm() {
    super();
  }

  public RadioButtonQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.RADIO_BUTTON;
  }
}
