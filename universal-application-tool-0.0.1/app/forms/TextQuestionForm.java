package forms;

import java.util.OptionalInt;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;

public class TextQuestionForm extends QuestionForm {
  private OptionalInt minLength;
  private OptionalInt maxLength;

  public TextQuestionForm() {
    super();
    setQuestionType(QuestionType.TEXT);
    minLength = OptionalInt.empty();
    maxLength = OptionalInt.empty();
  }

  public TextQuestionForm(TextQuestionDefinition qd) {
    super(qd);
    setQuestionType(QuestionType.TEXT);
    minLength = qd.getMinLength();
    maxLength = qd.getMaxLength();
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
    if (minLengthAsString.isEmpty()) {
      this.minLength = OptionalInt.empty();
    } else {
      this.minLength = OptionalInt.of(Integer.parseInt(minLengthAsString));
    }
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
    if (maxLengthAsString.isEmpty()) {
      this.maxLength = OptionalInt.empty();
    } else {
      this.maxLength = OptionalInt.of(Integer.parseInt(maxLengthAsString));
    }
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    TextQuestionDefinition.TextValidationPredicates.Builder textValidationPredicatesBuilder =
        TextQuestionDefinition.TextValidationPredicates.builder();

    if (getMinLength().isPresent()) {
      textValidationPredicatesBuilder.setMinLength(getMinLength().getAsInt());
    }

    if (getMaxLength().isPresent()) {
      textValidationPredicatesBuilder.setMaxLength(getMaxLength().getAsInt());
    }

    return super.getBuilder().setValidationPredicates(textValidationPredicatesBuilder.build());
  }
}
