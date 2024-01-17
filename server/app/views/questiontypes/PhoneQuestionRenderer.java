package views.questiontypes;

import static j2html.TagCreator.div;

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

public class PhoneQuestionRenderer extends ApplicantCompositeQuestionRenderer {
  PhoneQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-phone";
  }

  @Override
  protected DivTag renderInputTags(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      boolean isOptional) {
    PhoneQuestion phoneQuestion = applicantQuestion.createPhoneQuestion();

    Messages messages = params.messages();

    SelectWithLabel countryCodeField =
        (SelectWithLabel)
            new SelectWithLabel()
                .addStyleClass("py-15")
                .setFieldName(phoneQuestion.getCountryCodePath().toString())
                .setValue(phoneQuestion.getCountryCodeValue().orElse(""))
                .setLabelText(messages.at(MessageKey.PHONE_LABEL_COUNTRY_CODE.getKeyName()))
                .setOptionGroups(
                    ImmutableList.of(
                        SelectWithLabel.OptionGroup.builder()
                            .setLabel(messages.at(MessageKey.PHONE_LABEL_COUNTRY_CODE.getKeyName()))
                            .setOptions(COUNTRY_OPTIONS)
                            .build()))
                .setAriaRequired(!isOptional)
                .setFieldErrors(
                    messages,
                    validationErrors.getOrDefault(
                        phoneQuestion.getCountryCodePath(), ImmutableSet.of()))
                .addReferenceClass(ReferenceClasses.PHONE_COUNTRY_CODE)
                .setId(ReferenceClasses.PHONE_COUNTRY_CODE);

    boolean alreadyAutofocused = false;
    if (params.autofocusFirstField()
        || (params.autofocusFirstError()
            && validationErrors.containsKey(phoneQuestion.getCountryCodePath()))) {
      alreadyAutofocused = true;
      countryCodeField.focusOnInput();
    }

    FieldWithLabel phoneField =
        FieldWithLabel.input()
            .setPlaceholderText("(xxx) xxx-xxxx")
            .setFieldName(phoneQuestion.getPhoneNumberPath().toString())
            .setAttribute("inputmode", "tel")
            .setValue(phoneQuestion.getPhoneNumberValue().orElse(""))
            .setLabelText(messages.at(MessageKey.PHONE_LABEL_PHONE_NUMBER.getKeyName()))
            .setAriaRequired(!isOptional)
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(
                    phoneQuestion.getPhoneNumberPath(), ImmutableSet.of()))
            .setScreenReaderText(applicantQuestion.getQuestionTextForScreenReader())
            .addReferenceClass(ReferenceClasses.PHONE_NUMBER)
            .setId(ReferenceClasses.PHONE_NUMBER);

    if (!alreadyAutofocused
        && params.autofocusFirstError()
        && validationErrors.containsKey(phoneQuestion.getCountryCodePath())) {
      countryCodeField.focusOnInput();
    }

    if (!validationErrors.isEmpty()) {
      countryCodeField.forceAriaInvalid();
      phoneField.forceAriaInvalid();
    }

    return div()
        .withClasses("grid")
        .with(countryCodeField.getSelectTag(), phoneField.getInputTag());
  }

  private static final ImmutableList<SelectWithLabel.OptionValue> COUNTRY_OPTIONS =
      ImmutableList.of(
          SelectWithLabel.OptionValue.builder().setLabel("United States").setValue("US").build(),
          SelectWithLabel.OptionValue.builder().setLabel("Canada").setValue("CA").build());
}
