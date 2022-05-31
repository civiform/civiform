package forms;

import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a name question. */
public class NameQuestionForm extends QuestionForm {
  public NameQuestionForm() {
    super();
  }

  public NameQuestionForm(NameQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NAME;
  }
}
