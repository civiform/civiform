package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;

import java.util.Optional;
import java.util.stream.Stream;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NameQuestion;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
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

  public enum nameSuffixEnum {
    JUNIOR,
    SENIOR,
    II,
    III
}

  @Override
  protected DivTag renderInputTags(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      boolean isOptional) {
    Messages messages = params.messages();
    NameQuestion nameQuestion = applicantQuestion.createNameQuestion();
    boolean alreadyAutofocused = false;

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
            .addReferenceClass(ReferenceClasses.NAME_FIRST);
    if (params.autofocusFirstField()
        || (params.autofocusFirstError()
            && validationErrors.containsKey(nameQuestion.getFirstNamePath()))) {
      alreadyAutofocused = true;
      firstNameField.focusOnInput();
    }

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

    SelectWithLabel nameSuffixField = 
            new SelectWithLabel()
            .addReferenceClass("cf-dropdown-question")
            .setLabelText("Name suffix")
            .setFieldName(nameQuestion.getNameSuffixPath().toString())
            .setPlaceholderText(messages.at(MessageKey.DROPDOWN_PLACEHOLDER.getKeyName()))
            .setOptions(
              Stream.of(nameSuffixEnum.values())
              .map(
                option -> SelectWithLabel.OptionValue.builder()
                          .setLabel(option.toString())
                          .setValue(option.toString())
                          .build())
              .collect(ImmutableList.toImmutableList())
            );

    if (!alreadyAutofocused
        && params.autofocusFirstError()
        && validationErrors.containsKey(nameQuestion.getLastNamePath())) {
      lastNameField.focusOnInput();
    }

    if (!validationErrors.isEmpty()) {
      firstNameField.forceAriaInvalid();
      lastNameField.forceAriaInvalid();
    }

    DivTag nameQuestionFormContent =
        div()
            .with(firstNameField.getInputTag())
            .with(middleNameField.getInputTag())
            .with(lastNameField.getInputTag())
            .with(nameSuffixField.getSelectTag());

    return nameQuestionFormContent;
  }
}
