package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import j2html.tags.Tag;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.CurrencyQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

public class CurrencyQuestionRenderer extends ApplicantQuestionRenderer {

  public CurrencyQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.CURRENCY_QUESTION;
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    CurrencyQuestion currencyQuestion = question.createCurrencyQuestion();

    FieldWithLabel currencyField =
        FieldWithLabel.currency()
            .setFieldName(currencyQuestion.getCurrencyPath().toString())
            .addReferenceClass(ReferenceClasses.CURRENCY_QUESTION)
            .setScreenReaderText(question.getQuestionText())
            .setFieldErrors(params.messages(), currencyQuestion.getQuestionErrors())
            .showFieldErrors(false);
    if (currencyQuestion.getCurrencyValue().isPresent()) {
      // Convert currency cents to dollars.
      OptionalDouble value = OptionalDouble.of(currencyQuestion.getCurrencyValue().orElse(0L) / 100.0);
      currencyField.setValue(value);
    }

    Tag currencyQuestionFormContent = div().withClasses(Styles.FLEX)
        .with(div().withText("$"))
        .with(currencyField.getContainer());

    return renderInternal(params.messages(), currencyQuestionFormContent, false);
  }
}
