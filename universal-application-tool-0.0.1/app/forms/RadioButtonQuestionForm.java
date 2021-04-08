package forms;

import services.question.QuestionType;
import services.question.types.RadioButtonQuestionDefinition;

public class RadioButtonQuestionForm extends MultiOptionQuestionForm {

  public RadioButtonQuestionForm() {
    super(QuestionType.RADIO_BUTTON);
  }

  public RadioButtonQuestionForm(RadioButtonQuestionDefinition qd) {
    super(qd);
  }
}
