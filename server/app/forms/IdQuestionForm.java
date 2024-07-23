package forms;

import java.util.OptionalInt;
import services.question.types.IdQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating an id question. */
public class IdQuestionForm extends QuestionForm {
  private OptionalInt minLength;
  private OptionalInt maxLength;

  public IdQuestionForm() {
    super();
    this.minLength = OptionalInt.empty();
    this.maxLength = OptionalInt.empty();
  }

  public IdQuestionForm(IdQuestionDefinition qd) {
    super(qd);
    this.minLength = qd.getMinLength();
    this.maxLength = qd.getMaxLength();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ID;
  }

  public OptionalInt getMinLength() {
    return minLength;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMinLength(String minLengthAsString) {
    this.minLength =
        minLengthAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(minLengthAsString));
  }

  public OptionalInt getMaxLength() {
    return maxLength;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMaxLength(String maxLengthAsString) {
    this.maxLength =
        maxLengthAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(maxLengthAsString));
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    IdQuestionDefinition.IdValidationPredicates.Builder idValidationPredicatesBuilder =
        IdQuestionDefinition.IdValidationPredicates.builder();

    idValidationPredicatesBuilder.setMinLength(getMinLength());
    idValidationPredicatesBuilder.setMaxLength(getMaxLength());

    return super.getBuilder().setValidationPredicates(idValidationPredicatesBuilder.build());
  }
}
