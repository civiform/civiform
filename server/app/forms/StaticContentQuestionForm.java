package forms;

import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a static content question. */
public class StaticContentQuestionForm extends QuestionForm {

  public StaticContentQuestionForm() {
    super();
  }

  public StaticContentQuestionForm(QuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.STATIC;
  }
}
