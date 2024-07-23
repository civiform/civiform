package forms;

import java.util.OptionalLong;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating a number question. */
public class NumberQuestionForm extends QuestionForm {
  private OptionalLong min;
  private OptionalLong max;

  public NumberQuestionForm() {
    super();
    this.min = OptionalLong.empty();
    this.max = OptionalLong.empty();
  }

  public NumberQuestionForm(NumberQuestionDefinition qd) {
    super(qd);
    this.min = qd.getMin();
    this.max = qd.getMax();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NUMBER;
  }

  public OptionalLong getMin() {
    return min;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalLong. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMin(String minAsString) {
    this.min =
        minAsString.isEmpty() ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(minAsString));
  }

  public OptionalLong getMax() {
    return max;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalLong. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMax(String maxAsString) {
    this.max =
        maxAsString.isEmpty() ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(maxAsString));
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    NumberQuestionDefinition.NumberValidationPredicates.Builder numberValidationPredicatesBuilder =
        NumberQuestionDefinition.NumberValidationPredicates.builder();

    numberValidationPredicatesBuilder.setMin(getMin());
    numberValidationPredicatesBuilder.setMax(getMax());

    return super.getBuilder().setValidationPredicates(numberValidationPredicatesBuilder.build());
  }
}
