package forms;

import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a checkbox question. */
public class CheckboxQuestionForm extends MultiOptionQuestionForm {

  public CheckboxQuestionForm() {
    super();
  }

  public CheckboxQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.CHECKBOX;
  }
}
