package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.Tag;
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
  public Tag render() {
    NameQuestion nameQuestion = question.createNameQuestion();

    return div()
        .withId(question.getPath().path())
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
                        .setFieldName(nameQuestion.getFirstNamePath().path())
                        .setLabelText("First name")
                        .setPlaceholderText("First name")
                        .setFloatLabel(true)
                        .setValue(nameQuestion.getFirstNameValue().orElse(""))
                        .getContainer())
                .with(fieldErrors(nameQuestion.getFirstNameErrors()))
                .with(
                    FieldWithLabel.input()
                        .setFieldName(nameQuestion.getMiddleNamePath().path())
                        .setLabelText("Middle name")
                        .setPlaceholderText("Middle name")
                        .setFloatLabel(true)
                        .setValue(nameQuestion.getMiddleNameValue().orElse(""))
                        .getContainer())
                .with(
                    FieldWithLabel.input()
                        .setFieldName(nameQuestion.getLastNamePath().path())
                        .setLabelText("Last name")
                        .setPlaceholderText("Last name")
                        .setFloatLabel(true)
                        .setValue(nameQuestion.getLastNameValue().orElse(""))
                        .getContainer())
                .with(fieldErrors(nameQuestion.getLastNameErrors())));
  }
}
