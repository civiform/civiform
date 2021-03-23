package forms;

import java.util.OptionalInt;
import services.question.TextQuestionDefinition;

public class TextQuestionForm extends QuestionForm {
  private OptionalInt textMinLength;
  private OptionalInt textMaxLength;

  public TextQuestionForm() {
    super();
    textMinLength = OptionalInt.empty();
    textMaxLength = OptionalInt.empty();
  }

  public TextQuestionForm(TextQuestionDefinition qd) {
    super();
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
}
