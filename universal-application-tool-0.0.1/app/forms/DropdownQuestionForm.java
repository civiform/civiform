package forms;

import services.question.DropdownQuestionDefinition;
import services.question.QuestionType;

public class DropdownQuestionForm extends MultiOptionQuestionForm {

  public DropdownQuestionForm() {
    super(QuestionType.DROPDOWN);
  }

  public DropdownQuestionForm(DropdownQuestionDefinition qd) {
    super(qd);
  }
}
