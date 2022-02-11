package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import services.MessageKey;
import services.applicant.ValidationErrorMessage;
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
            .addReferenceClass(ReferenceClasses.CURRENCY_VALUE)
            .setScreenReaderText(question.getQuestionText())
            .setFieldErrors(
                params.messages(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.CURRENCY_VALIDATION_MISFORMATTED)))
            .setDescribedByHtmlId(questionHelpTextHtmlId())
            .showFieldErrors(false);
    if (currencyQuestion.getValue().isPresent()) {
      currencyField.setValue(currencyQuestion.getValue().get());
    }

    Tag dollarSign =
        div()
            .withText("$")
            .withClasses(
                Styles.FLEX,
                Styles.ITEMS_CENTER,
                // Same height and padding as the input field.
                Styles.H_12,
                Styles.MB_2,
                // Pad the right side.
                Styles.MR_2,
                // Same text as the input field.
                Styles.TEXT_LG);

    Tag currencyQuestionFormContent =
        div().withClasses(Styles.FLEX).with(dollarSign).with(currencyField.getContainer());

    return renderInternal(params.messages(), currencyQuestionFormContent);
  }
}
