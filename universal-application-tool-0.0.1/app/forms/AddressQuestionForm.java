package forms;

import services.question.AddressQuestionDefinition;

public class AddressQuestionForm extends QuestionForm {
  public AddressQuestionForm() {
    super();
    // TODO(#590): Switch to QuestionType instead of string
    setQuestionType("ADDRESS");
  }

  public AddressQuestionForm(AddressQuestionDefinition qd) {
    super(qd);
    // TODO(#590): Switch to QuestionType instead of string
    setQuestionType("ADDRESS");
  }
}
