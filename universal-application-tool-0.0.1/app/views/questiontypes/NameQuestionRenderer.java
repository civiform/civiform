package views.questiontypes;

import static j2html.TagCreator.div;

import j2html.tags.Tag;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NameQuestion;
import views.components.FieldWithLabel;

public class NameQuestionRenderer extends ApplicantQuestionRenderer {

  public NameQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    NameQuestion nameQuestion = question.createNameQuestion();

    Tag nameQuestionFormContent =
        div()
            .with(
                FieldWithLabel.input()
                    .setFieldName(nameQuestion.getFirstNamePath().toString())
                    .setLabelText(messages.at(MessageKey.NAME_LABEL_FIRST.getKeyName()))
                    .setValue(nameQuestion.getFirstNameValue().orElse(""))
                    .setFieldErrors(messages, nameQuestion.getFirstNameErrors())
                    .getContainer())
            .with(
                FieldWithLabel.input()
                    .setFieldName(nameQuestion.getMiddleNamePath().toString())
                    .setLabelText(messages.at(MessageKey.NAME_LABEL_MIDDLE.getKeyName()))
                    .setValue(nameQuestion.getMiddleNameValue().orElse(""))
                    .getContainer())
            .with(
                FieldWithLabel.input()
                    .setFieldName(nameQuestion.getLastNamePath().toString())
                    .setLabelText(messages.at(MessageKey.NAME_LABEL_LAST.getKeyName()))
                    .setValue(nameQuestion.getLastNameValue().orElse(""))
                    .setFieldErrors(messages, nameQuestion.getLastNameErrors())
                    .getContainer());

    return renderInternal(messages, nameQuestionFormContent);
  }
}
