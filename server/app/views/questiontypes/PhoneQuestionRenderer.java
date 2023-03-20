package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.PhoneQuestion;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.style.ReferenceClasses;

import static j2html.TagCreator.div;

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
    SelectWithLabel countryCodeField =
      (SelectWithLabel)
        new SelectWithLabel()
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
          .addReferenceClass(ReferenceClasses.PHONE_COUNTRY_CODE);
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
        .addReferenceClass(ReferenceClasses.PHONE_NUMBER);

    if (!validationErrors.isEmpty()) {
      countryCodeField.forceAriaInvalid();
      phoneField.forceAriaInvalid();

    }

   return div().with(countryCodeField.getSelectTag(), phoneField.getInputTag());
  }

  private static ImmutableList<SelectWithLabel.OptionValue> countryOptions() {
    return ImmutableList.of(
      SelectWithLabel.OptionValue.builder().setLabel("United States").setValue("US").build(),
      SelectWithLabel.OptionValue.builder().setLabel("Canada").setValue("CA").build());
  }
}
