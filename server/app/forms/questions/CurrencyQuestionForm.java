package forms.questions;

import services.question.types.CurrencyQuestionDefinition;
import services.question.types.QuestionType;

public final class CurrencyQuestionForm extends QuestionForm {

  public CurrencyQuestionForm() {
    super();
  }

  public CurrencyQuestionForm(CurrencyQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.CURRENCY;
  }
}
