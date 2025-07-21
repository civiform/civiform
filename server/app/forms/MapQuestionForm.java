package forms;

import java.util.OptionalInt;
import services.question.types.MapQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class MapQuestionForm extends QuestionForm {

  private OptionalInt minChoicesRequired;
  private OptionalInt maxChoicesAllowed;

  public MapQuestionForm() {
    super();
    this.minChoicesRequired = OptionalInt.empty();
    this.maxChoicesAllowed = OptionalInt.empty();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.MAP;
  }

  /**
   * Build a QuestionForm from a {@link QuestionDefinition}, to build the QuestionEditView.
   *
   * @param qd the {@link QuestionDefinition} from which to build a QuestionForm
   */
  public MapQuestionForm(MapQuestionDefinition qd) {
    super(qd);
    this.minChoicesRequired = qd.getMapValidationPredicates().minChoicesRequired();
    this.maxChoicesAllowed = qd.getMapValidationPredicates().maxChoicesAllowed();
  }

  public OptionalInt getMinChoicesRequired() {
    return minChoicesRequired;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMinChoicesRequired(String minChoicesRequiredAsString) {
    this.minChoicesRequired =
        minChoicesRequiredAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(minChoicesRequiredAsString));
  }

  public OptionalInt getMaxChoicesAllowed() {
    return maxChoicesAllowed;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMaxChoicesAllowed(String maxChoicesAllowedAsString) {
    this.maxChoicesAllowed =
        maxChoicesAllowedAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(maxChoicesAllowedAsString));
  }

  /**
   * Build a {@link QuestionDefinitionBuilder} from this QuestionForm, for handling the form
   * response.
   *
   * @return a {@link QuestionDefinitionBuilder} with the values from this QuestionForm
   */
  @Override
  public QuestionDefinitionBuilder getBuilder() {
    MapQuestionDefinition.MapValidationPredicates.Builder predicateBuilder =
        MapQuestionDefinition.MapValidationPredicates.builder();

    if (getMinChoicesRequired().isPresent()) {
      predicateBuilder.setMinChoicesRequired(getMinChoicesRequired());
    }

    if (getMaxChoicesAllowed().isPresent()) {
      predicateBuilder.setMaxChoicesAllowed(getMaxChoicesAllowed());
    }

    return super.getBuilder().setValidationPredicates(predicateBuilder.build());
  }
}
