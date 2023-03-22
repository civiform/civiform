package forms;

import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionType;

public class PhoneQuestionForm extends QuestionForm {
  public PhoneQuestionForm() {
    super();
  }

  public PhoneQuestionForm(PhoneQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.PHONE;
  }
}
