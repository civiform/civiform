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

  public void setMinLength(int minLength) {
    this.minLength = OptionalInt.of(minLength);
  }

  public OptionalInt getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(int maxLength) {
    this.maxLength = OptionalInt.of(maxLength);
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
