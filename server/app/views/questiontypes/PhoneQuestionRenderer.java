package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.PhoneQuestion;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

import static j2html.TagCreator.*;

public class PhoneQuestionRenderer extends ApplicantSingleQuestionRenderer {
  PhoneQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }
  @Override
  public String getReferenceClass() {
    return "cf-question-phone";
  }
  @Override
  protected DivTag renderInputTag(
    ApplicantQuestionRendererParams params,
    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
    ImmutableList<String> ariaDescribedByIds) {
    PhoneQuestion phoneQuestion = question.createPhoneQuestion();

    Messages messages = params.messages();
    OptionTag optionTagUS = option("United States").withValue("US");
    OptionTag optionTagCA = option("Canada").withValue("CA");


    SelectWithLabel countryCodeField =
      (SelectWithLabel)
        new SelectWithLabel()
          .setFieldName(phoneQuestion.getCountryCodePath().toString())
          .setValue(phoneQuestion.getCountryCodeValue().orElse(""))
          .setLabelText(messages.at(MessageKey.PHONE_LABEL_COUNTRY_CODE.getKeyName()))
          .setCustomOptions(ImmutableList.of(optionTagCA,optionTagUS))
          .setFieldErrors(
            messages,
            validationErrors.getOrDefault(
              phoneQuestion.getCountryCodePath(), ImmutableSet.of()))
          .setId(ReferenceClasses.PHONE_COUNTRY_CODE);

    FieldWithLabel phoneField =
      FieldWithLabel.input()
        .setPlaceholderText("(xxx) xxx-xxxx")
        .setFieldName(phoneQuestion.getPhoneNumberPath().toString())
        .setValue(phoneQuestion.getPhoneNumberValue().orElse(""))
        .setLabelText(messages.at(MessageKey.PHONE_LABEL_PHONE_NUMBER.getKeyName()))
        .setFieldErrors(
          messages,
          validationErrors.getOrDefault(phoneQuestion.getPhoneNumberPath(), ImmutableSet.of()))
        .setAriaDescribedByIds(ariaDescribedByIds)
        .setScreenReaderText(question.getQuestionTextForScreenReader())
        .setId(ReferenceClasses.PHONE_NUMBER);

    if (!validationErrors.isEmpty()) {
      countryCodeField.forceAriaInvalid();
      phoneField.forceAriaInvalid();

    }

   return div()
     .withClasses("grid","grid-column:auto")
      .with(
        countryCodeField.getSelectTag(), phoneField.getInputTag());
  }

}
