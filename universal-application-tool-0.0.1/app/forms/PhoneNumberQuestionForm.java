package forms;

import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.PhoneNumberQuestionDefinition;

public class PhoneNumberQuestionForm extends QuestionForm {

  public PhoneNumberQuestionForm() {
    super();
  }

  public PhoneNumberQuestionForm(PhoneNumberQuestionDefinition questionDefinition) {
    super(questionDefinition);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.PHONENUMBER;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    PhoneNumberQuestionDefinition.PhoneNumberValidationPredicates.Builder phoneNumberValidationPredicatesBuilder =
        PhoneNumberQuestionDefinition.PhoneNumberValidationPredicates.builder();

    return super.getBuilder().setValidationPredicates(phoneNumberValidationPredicatesBuilder.build());
  }
}
