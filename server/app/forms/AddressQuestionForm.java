package forms;

import java.util.Optional;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating an address question. */
public class AddressQuestionForm extends QuestionForm {
  private boolean disallowPoBox;

  private Optional<String> defaultState;

  public AddressQuestionForm() {
    super();
    this.disallowPoBox = false;
    this.defaultState = Optional.empty();
  }

  public AddressQuestionForm(AddressQuestionDefinition qd) {
    super(qd);
    this.disallowPoBox = qd.getDisallowPoBox();
    this.defaultState = qd.getDefaultState();
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

  public void setDefaultState(String defaultState) {
    this.defaultState = Optional.ofNullable(defaultState);
  }

  public Optional<String> getDefaultState() {
    return defaultState;
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
