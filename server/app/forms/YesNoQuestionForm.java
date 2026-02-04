package forms;

import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a yes/no question. */
public class YesNoQuestionForm extends MultiOptionQuestionForm {

  public YesNoQuestionForm() {
    super();
  }

  public YesNoQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.YES_NO;
  }
}
