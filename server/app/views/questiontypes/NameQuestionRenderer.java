package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NameQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/** Renders a name question. */
public class NameQuestionRenderer extends ApplicantQuestionRendererImpl {

  public NameQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.NAME_QUESTION;
  }

  @Override
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    NameQuestion nameQuestion = question.createNameQuestion();

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors =
        nameQuestion.getValidationErrors();

    Tag nameQuestionFormContent =
        div()
            .with(
                FieldWithLabel.input()
                    .setFieldName(nameQuestion.getFirstNamePath().toString())
                    .setLabelText(messages.at(MessageKey.NAME_LABEL_FIRST.getKeyName()))
                    .setValue(nameQuestion.getFirstNameValue().orElse(""))
                    .setFieldErrors(messages, nameQuestion.getFirstNameErrorMessage())
                    .showFieldErrors(
                        !validationErrors
                            .getOrDefault(nameQuestion.getFirstNamePath(), ImmutableSet.of())
                            .isEmpty())
                    .addReferenceClass(ReferenceClasses.NAME_FIRST)
                    .getContainer())
            .with(
                FieldWithLabel.input()
                    .setFieldName(nameQuestion.getMiddleNamePath().toString())
                    .setLabelText(messages.at(MessageKey.NAME_LABEL_MIDDLE.getKeyName()))
                    .setValue(nameQuestion.getMiddleNameValue().orElse(""))
                    .addReferenceClass(ReferenceClasses.NAME_MIDDLE)
                    .getContainer())
            .with(
                FieldWithLabel.input()
                    .setFieldName(nameQuestion.getLastNamePath().toString())
                    .setLabelText(messages.at(MessageKey.NAME_LABEL_LAST.getKeyName()))
                    .setValue(nameQuestion.getLastNameValue().orElse(""))
                    .setFieldErrors(messages, nameQuestion.getLastNameErrorMessage())
                    .showFieldErrors(
                        !validationErrors
                            .getOrDefault(nameQuestion.getLastNamePath(), ImmutableSet.of())
                            .isEmpty())
                    .addReferenceClass(ReferenceClasses.NAME_LAST)
                    .getContainer());

    return nameQuestionFormContent;
  }
}
