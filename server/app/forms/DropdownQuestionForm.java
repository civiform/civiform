package forms;

import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a dropdown question. */
public class DropdownQuestionForm extends MultiOptionQuestionForm {

  public DropdownQuestionForm() {
    super();
  }

  public DropdownQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DROPDOWN;
  }
}
