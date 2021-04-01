package forms;

import java.util.OptionalInt;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TextQuestionDefinition;

public class TextQuestionForm extends QuestionForm {
  private OptionalInt minLength;
  private OptionalInt maxLength;

  public TextQuestionForm() {
    super();
    setQuestionType(QuestionType.TEXT);
    this.minLength = OptionalInt.empty();
    this.maxLength = OptionalInt.empty();
  }

  public TextQuestionForm(TextQuestionDefinition qd) {
    super(qd);
    setQuestionType(QuestionType.TEXT);
    this.minLength = qd.getMinLength();
    this.maxLength = qd.getMaxLength();
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

    textValidationPredicatesBuilder.setMinLength(getMinLength());
    textValidationPredicatesBuilder.setMaxLength(getMaxLength());

    return super.getBuilder().setValidationPredicates(textValidationPredicatesBuilder.build());
  }
}
