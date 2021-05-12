package forms;

import services.Path;
import services.question.types.DateQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class DateQuestionForm extends QuestionForm {

  public DateQuestionForm() {
    super();
  }

  public DateQuestionForm(DateQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DATE;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    //        DateQuestionDefinition.DateValidationPredicates.Builder
    // dateValidationPredicatesBuilder =
    //                DateQuestionDefinition.DateValidationPredicates.create();
    //
    //        return super.getBuilder(path)
    //                .setValidationPredicates(dateValidationPredicatesBuilder.build());

    return null;
  }
}
