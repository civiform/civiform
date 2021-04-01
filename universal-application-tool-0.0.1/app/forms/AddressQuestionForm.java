package forms;

import services.question.AddressQuestionDefinition;
import services.question.QuestionType;

public class AddressQuestionForm extends QuestionForm {
  // TODO: Add address question configurations and validations here.

  public AddressQuestionForm() {
    super();
    setQuestionType(QuestionType.ADDRESS);
  }

  public AddressQuestionForm(AddressQuestionDefinition qd) {
    super(qd);
    setQuestionType(QuestionType.ADDRESS);
  }
}
