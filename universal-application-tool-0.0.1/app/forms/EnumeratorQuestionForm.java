package forms;

import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionType;

public class EnumeratorQuestionForm extends QuestionForm {
  public EnumeratorQuestionForm() {
    super();
  }

  public EnumeratorQuestionForm(EnumeratorQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ENUMERATOR;
  }
}
