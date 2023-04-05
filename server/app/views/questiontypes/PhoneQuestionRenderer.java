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

  public static final String US_LABEL = "United States";
  public static final String CA_LABEL = "Canada";
  public static final String US_VALUE = "US";
  public static final String CA_VALUE = "CA";

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
//    OptionTag optionTagUS = option(US_LABEL).withValue(US_VALUE);
//    OptionTag optionTagCA = option(CA_LABEL).withValue(CA_VALUE);
//    String selectedOption = "";
//
//if(phoneQuestion.getCountryCodeValue().isPresent()) {
//  String value = phoneQuestion.getCountryCodeValue().get();
//  if (value.equals(US_VALUE)) {
//    selectedOption = US_LABEL;
//  } else {
//    selectedOption = CA_LABEL;
//  }
//}
    SelectWithLabel countryCodeField =
      (SelectWithLabel)
    new SelectWithLabel().addStyleClass("py-15")
      .setFieldName(phoneQuestion.getCountryCodePath().toString())
      .setValue(phoneQuestion.getCountryCodeValue().orElse(""))
      .setLabelText(messages.at(MessageKey.PHONE_LABEL_COUNTRY_CODE.getKeyName()))
      .setOptionGroups(
        ImmutableList.of(
          SelectWithLabel.OptionGroup.builder()
            .setLabel(
              messages.at(MessageKey.PHONE_LABEL_COUNTRY_CODE.getKeyName()))
            .setOptions(countryOptions())
            .build()))
      .setFieldErrors(
        messages,
        validationErrors.getOrDefault(
          phoneQuestion.getCountryCodePath(), ImmutableSet.of()))
      .setId(ReferenceClasses.PHONE_COUNTRY_CODE);



//    SelectWithLabel countryCodeField =
//      (SelectWithLabel)
//        new SelectWithLabel()
//          .setFieldName(phoneQuestion.getCountryCodePath().toString())
//          .setPlaceholderText(selectedOption)
//          .setValue(phoneQuestion.getCountryCodeValue().orElse(""))
//          .setLabelText(messages.at(MessageKey.PHONE_LABEL_COUNTRY_CODE.getKeyName()))
//          .setCustomOptions(ImmutableList.of(optionTagCA,optionTagUS))
//          .setFieldErrors(
//            messages,
//            validationErrors.getOrDefault(
//              phoneQuestion.getCountryCodePath(), ImmutableSet.of()))
//          .setId(ReferenceClasses.PHONE_COUNTRY_CODE);

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
     .withClasses("grid")
      .with(
        countryCodeField.getSelectTag(), phoneField.getInputTag());
  }

  private static ImmutableList<SelectWithLabel.OptionValue> countryOptions() {
    return ImmutableList.of(
      SelectWithLabel.OptionValue.builder().setLabel("United States").setValue("US").build(),
      SelectWithLabel.OptionValue.builder().setLabel("Canada").setValue("CA").build());
  }

}
