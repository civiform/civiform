package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import java.util.UUID;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.PhoneQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

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
      ImmutableList<String> ariaDescribedByIds,
      boolean isOptional) {
    PhoneQuestion phoneQuestion = applicantQuestion.createPhoneQuestion();

    Messages messages = params.messages();

    // Generating a unique id for the input element. The phone.ts script needs to find the input
    // element, but the ReferenceClasses point to an outer container element. Relying on finding
    // the correct input element inside the container is fragile and will break if additional
    // form elements are added later.
    String phoneFieldId = String.format("%s-%s", ReferenceClasses.PHONE_NUMBER, UUID.randomUUID());

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
            .addReferenceClass(getReferenceClass())
            .setId(phoneFieldId);

    if (params.autofocusFirstField() || params.autofocusFirstError()) {
      phoneField.focusOnInput();
    }

    if (!validationErrors.isEmpty()) {
      phoneField.forceAriaInvalid();
    }

    return div().with(phoneField.getInputTag());
  }
}
