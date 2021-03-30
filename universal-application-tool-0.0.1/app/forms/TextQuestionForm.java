package forms;

import java.util.OptionalInt;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TextQuestionDefinition;

//public class TextQuestionForm extends QuestionForm {
//  private OptionalInt textMinLength;
//  private OptionalInt textMaxLength;
//
//  public TextQuestionForm() {
//    super();
//    textMinLength = OptionalInt.empty();
//    textMaxLength = OptionalInt.empty();
//    setQuestionType(QuestionType.TEXT);
//  }
//
//  public TextQuestionForm(TextQuestionDefinition qd) {
//    super(qd);
//    textMinLength = qd.getMinLength();
//    textMaxLength = qd.getMaxLength();
//  }
//
//  public OptionalInt getTextMinLength() {
//    return textMinLength;
//  }
//
//  public void setTextMinLength(int textMinLength) {
//    this.textMinLength = OptionalInt.of(textMinLength);
//  }
//
//  public OptionalInt getTextMaxLength() {
//    return textMaxLength;
//  }
//
//  public void setTextMaxLength(int textMaxLength) {
//    this.textMaxLength = OptionalInt.of(textMaxLength);
//  }
//
//  @Override
//  public QuestionDefinitionBuilder getBuilder() {
//    TextQuestionDefinition.TextValidationPredicates.Builder textValidationPredicatesBuilder =
//        TextQuestionDefinition.TextValidationPredicates.builder();
//
//    if (getTextMinLength().isPresent()) {
//      textValidationPredicatesBuilder.setMinLength(getTextMinLength().getAsInt());
//    }
//
//    if (getTextMaxLength().isPresent()) {
//      textValidationPredicatesBuilder.setMaxLength(getTextMaxLength().getAsInt());
//    }
//
//    return super.getBuilder().setValidationPredicates(textValidationPredicatesBuilder.build());
//  }
//}

public class TextQuestionForm extends QuestionForm {
  private String textMinLength;
  private String textMaxLength;

  public TextQuestionForm() {
    super();
    textMinLength = "";
    textMaxLength = "";
    setQuestionType(QuestionType.TEXT);
  }

  public TextQuestionForm(TextQuestionDefinition qd) {
    super(qd);
    textMinLength = qd.getMinLength().toString();
    textMaxLength = qd.getMaxLength().toString();
  }

  public String getTextMinLength() {
    System.out.println("get min");
    System.out.println(textMinLength);
    System.out.println(textMinLength.getClass());
    return textMinLength;
  }

  public void setTextMinLength(String textMinLength) {
    System.out.println("set min");
    System.out.println(textMinLength);
    this.textMinLength = textMinLength;
  }

  public String getTextMaxLength() {
    return textMaxLength;
  }

  public void setTextMaxLength(String textMaxLength) {
    this.textMaxLength = textMaxLength;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    TextQuestionDefinition.TextValidationPredicates.Builder textValidationPredicatesBuilder =
            TextQuestionDefinition.TextValidationPredicates.builder();

    if (!getTextMinLength().equals("")) {
      textValidationPredicatesBuilder.setMinLength(Integer.parseInt(getTextMinLength()));
    }

    if (!getTextMaxLength().equals("")) {
      textValidationPredicatesBuilder.setMaxLength(Integer.parseInt(getTextMaxLength()));
    }

    return super.getBuilder().setValidationPredicates(textValidationPredicatesBuilder.build());
  }
}
