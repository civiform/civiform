package forms;

import java.util.OptionalInt;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TextQuestionDefinition;

public class TextQuestionForm extends QuestionForm {
  private OptionalInt textMinLength;
  private OptionalInt textMaxLength;

  public TextQuestionForm() {
    super();
    textMinLength = OptionalInt.empty();
    textMaxLength = OptionalInt.empty();
    setQuestionType(QuestionType.TEXT);
  }

  public TextQuestionForm(TextQuestionDefinition qd) {
    super(qd);
    textMinLength = qd.getMinLength();
    textMaxLength = qd.getMaxLength();
  }

  public OptionalInt getTextMinLength() {
    return textMinLength;
  }

  public void setTextMinLength(int textMinLength) {
    this.textMinLength = OptionalInt.of(textMinLength);
  }

  public OptionalInt getTextMaxLength() {
    return textMaxLength;
  }

  public void setTextMaxLength(int textMaxLength) {
    this.textMaxLength = OptionalInt.of(textMaxLength);
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    TextQuestionDefinition.TextValidationPredicates.Builder textValidationPredicatesBuilder =
        TextQuestionDefinition.TextValidationPredicates.builder();

    if (getTextMinLength().isPresent()) {
      textValidationPredicatesBuilder.setMinLength(getTextMinLength().getAsInt());
    }

    if (getTextMaxLength().isPresent()) {
      textValidationPredicatesBuilder.setMaxLength(getTextMaxLength().getAsInt());
    }

    return super.getBuilder().setValidationPredicates(textValidationPredicatesBuilder.build());
  }
}
