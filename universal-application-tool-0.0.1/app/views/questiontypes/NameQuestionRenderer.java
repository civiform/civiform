package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.Tag;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NameQuestion;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

public class NameQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public NameQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    NameQuestion nameQuestion = question.createNameQuestion();

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.PX_16)
        .with(
            div()
                .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT)
                .withText(question.getQuestionText()),
            div()
                .withClasses(
                    ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                    Styles.TEXT_BASE,
                    Styles.FONT_THIN,
                    Styles.MB_2)
                .withText(question.getQuestionHelpText()),
            div()
                .with(
                    FieldWithLabel.input()
                        .setFieldName(nameQuestion.getFirstNamePath().toString())
                        .setLabelText(messages.at(MessageKey.NAME_LABEL_FIRST.getKeyName()))
                        .setApplicantStyle(true)
                        .setValue(nameQuestion.getFirstNameValue().orElse(""))
                        .setFieldErrors(messages, nameQuestion.getFirstNameErrors())
                        .getContainer())
                .with(
                    FieldWithLabel.input()
                        .setFieldName(nameQuestion.getMiddleNamePath().toString())
                        .setLabelText(messages.at(MessageKey.NAME_LABEL_MIDDLE.getKeyName()))
                        .setApplicantStyle(true)
                        .setValue(nameQuestion.getMiddleNameValue().orElse(""))
                        .getContainer())
                .with(
                    FieldWithLabel.input()
                        .setFieldName(nameQuestion.getLastNamePath().toString())
                        .setLabelText(messages.at(MessageKey.NAME_LABEL_LAST.getKeyName()))
                        .setApplicantStyle(true)
                        .setValue(nameQuestion.getLastNameValue().orElse(""))
                        .setFieldErrors(messages, nameQuestion.getLastNameErrors())
                        .getContainer()));
  }
}
