package forms;

import services.question.types.DropdownQuestionDefinition;
import services.question.types.QuestionType;

public class DropdownQuestionForm extends MultiOptionQuestionForm {

  public DropdownQuestionForm() {
    super();
  }

  public DropdownQuestionForm(DropdownQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DROPDOWN;
  }
}
