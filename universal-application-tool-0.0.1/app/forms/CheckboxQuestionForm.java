package forms;

import services.question.CheckboxQuestionDefinition;
import services.question.QuestionType;

public class CheckboxQuestionForm extends MultiOptionQuestionForm {

  public CheckboxQuestionForm() {
    super(QuestionType.CHECKBOX);
  }

  public CheckboxQuestionForm(CheckboxQuestionDefinition qd) {
    super(qd);
  }
}
