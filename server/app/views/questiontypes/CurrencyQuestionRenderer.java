package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.CurrencyQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

public class CurrencyQuestionRenderer extends ApplicantQuestionRendererImpl {

  public CurrencyQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.CURRENCY_QUESTION;
  }

  @Override
  protected DivTag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    CurrencyQuestion currencyQuestion = question.createCurrencyQuestion();

    FieldWithLabel currencyField =
        FieldWithLabel.currency()
            .setFieldName(currencyQuestion.getCurrencyPath().toString())
            .addReferenceClass(ReferenceClasses.CURRENCY_VALUE)
            .setScreenReaderText(question.getQuestionText())
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(
                    currencyQuestion.getCurrencyPath(), ImmutableSet.of()));
    if (currencyQuestion.getCurrencyValue().isPresent()) {
      currencyField.setValue(currencyQuestion.getCurrencyValue().get().prettyPrint());
    } else {
      currencyField.setValue(
          currencyQuestion.getFailedUpdates().getOrDefault(currencyQuestion.getCurrencyPath(), ""));
    }

    DivTag dollarSign =
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

    DivTag currencyQuestionFormContent =
        div().withClasses(Styles.FLEX).with(dollarSign).with(currencyField.getCurrencyTag());

    return currencyQuestionFormContent;
  }
}
