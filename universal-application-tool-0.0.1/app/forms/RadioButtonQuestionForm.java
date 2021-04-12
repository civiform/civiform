package forms;

import services.question.types.QuestionType;
import services.question.types.RadioButtonQuestionDefinition;

public class RadioButtonQuestionForm extends MultiOptionQuestionForm {

  public RadioButtonQuestionForm() {
    super(QuestionType.RADIO_BUTTON);
  }

  public RadioButtonQuestionForm(RadioButtonQuestionDefinition qd) {
    super(qd);
  }
}
