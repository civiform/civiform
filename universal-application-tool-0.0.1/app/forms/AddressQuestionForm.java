package forms;

import services.Path;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class AddressQuestionForm extends QuestionForm {
  private boolean disallowPoBox;

  public AddressQuestionForm() {
    super();
    this.disallowPoBox = false;
  }

  public AddressQuestionForm(AddressQuestionDefinition qd) {
    super(qd);
    this.disallowPoBox = qd.getDisallowPoBox();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ADDRESS;
  }

  public boolean getDisallowPoBox() {
    return disallowPoBox;
  }

  public void setDisallowPoBox(boolean disallowPoBox) {
    this.disallowPoBox = disallowPoBox;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder(Path path) {
    AddressQuestionDefinition.AddressValidationPredicates.Builder
        addressValidationPredicatesBuilder =
            AddressQuestionDefinition.AddressValidationPredicates.builder();

    addressValidationPredicatesBuilder.setDisallowPoBox(getDisallowPoBox());

    return super.getBuilder(path)
        .setValidationPredicates(addressValidationPredicatesBuilder.build());
  }
}
