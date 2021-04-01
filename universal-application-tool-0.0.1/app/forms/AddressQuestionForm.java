package forms;

import services.question.AddressQuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

public class AddressQuestionForm extends QuestionForm {
  private boolean disallowPoBox;

  public AddressQuestionForm() {
    super();
    setQuestionType(QuestionType.ADDRESS);
    this.disallowPoBox = false;
  }

  public AddressQuestionForm(AddressQuestionDefinition qd) {
    super(qd);
    setQuestionType(QuestionType.ADDRESS);
    this.disallowPoBox = qd.getDisallowPoBox();
  }

  public boolean getDisallowPoBox() {
    return disallowPoBox;
  }

  public void setDisallowPoBox(boolean disallowPoBox) {
    this.disallowPoBox = disallowPoBox;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    AddressQuestionDefinition.AddressValidationPredicates.Builder
        addressValidationPredicatesBuilder =
            AddressQuestionDefinition.AddressValidationPredicates.builder();

    addressValidationPredicatesBuilder.setDisallowPoBox(getDisallowPoBox());

    return super.getBuilder().setValidationPredicates(addressValidationPredicatesBuilder.build());
  }
}
