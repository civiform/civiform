package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NameQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/** Renders a name question. */
public class NameQuestionRenderer extends ApplicantCompositeQuestionRenderer {

  public NameQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.NAME_QUESTION;
  }

  @Override
  protected DivTag renderInputTags(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      boolean isOptional) {
    Messages messages = params.messages();
    NameQuestion nameQuestion = applicantQuestion.createNameQuestion();

    FieldWithLabel firstNameField =
        FieldWithLabel.input()
            .setFieldName(nameQuestion.getFirstNamePath().toString())
            .setLabelText(messages.at(MessageKey.NAME_LABEL_FIRST.getKeyName()))
            .setAutocomplete(Optional.of("given-name"))
            .setValue(nameQuestion.getFirstNameValue().orElse(""))
            .setRequired(!isOptional)
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(nameQuestion.getFirstNamePath(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.NAME_FIRST)
            .maybeFocusOnInput(params, applicantQuestion);

    FieldWithLabel middleNameField =
        FieldWithLabel.input()
            .setFieldName(nameQuestion.getMiddleNamePath().toString())
            .setLabelText(messages.at(MessageKey.NAME_LABEL_MIDDLE.getKeyName()))
            .setAutocomplete(Optional.of("additional-name"))
            .setValue(nameQuestion.getMiddleNameValue().orElse(""))
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(nameQuestion.getMiddleNamePath(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.NAME_MIDDLE);

    FieldWithLabel lastNameField =
        FieldWithLabel.input()
            .setFieldName(nameQuestion.getLastNamePath().toString())
            .setLabelText(messages.at(MessageKey.NAME_LABEL_LAST.getKeyName()))
            .setAutocomplete(Optional.of("family-name"))
            .setValue(nameQuestion.getLastNameValue().orElse(""))
            .setRequired(!isOptional)
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(nameQuestion.getLastNamePath(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.NAME_LAST);

    if (!validationErrors.isEmpty()) {
      firstNameField.forceAriaInvalid();
      lastNameField.forceAriaInvalid();
      /* Currently, only the streetAddress field will ever receive focus given that
        we have no way of determining the exact field with an error. However, autofocus
        will be on each field when we eventually find a way to do that.
      */
      if (params
          .errorDisplayMode()
          .equals(ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_SINGLE_ERROR)) {
        firstNameField.focusOnError();
        middleNameField.focusOnError();
        lastNameField.focusOnError();
      }
    }

    DivTag nameQuestionFormContent =
        div()
            .with(firstNameField.getInputTag())
            .with(middleNameField.getInputTag())
            .with(lastNameField.getInputTag());

    return nameQuestionFormContent;
  }
}
