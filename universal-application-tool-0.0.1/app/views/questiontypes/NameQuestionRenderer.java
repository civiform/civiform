package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NameQuestion;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class NameQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public NameQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    NameQuestion nameQuestion = question.getNameQuestion();

    return div()
        .withId(question.getPath().path())
        .withClasses(Styles.MX_AUTO, Styles.W_MAX)
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
            label()
                .withClasses(Styles.BLOCK)
                .with(
                    input()
                        .withType("text")
                        .withPlaceholder("First name")
                        .withCondValue(
                            nameQuestion.hasFirstNameValue(),
                            nameQuestion.getFirstNameValue().orElse(""))
                        .withName(nameQuestion.getFirstNamePath().path()),
                    fieldErrors(nameQuestion.getFirstNameErrors())),
            label()
                .withClasses(Styles.BLOCK)
                .with(
                    input()
                        .withType("text")
                        .withPlaceholder("Middle name")
                        .withCondValue(
                            nameQuestion.hasMiddleNameValue(),
                            nameQuestion.getMiddleNameValue().orElse(""))
                        .withName(nameQuestion.getMiddleNamePath().path())),
            label()
                .withClasses(Styles.BLOCK)
                .with(
                    input()
                        .withType("text")
                        .withPlaceholder("Last name")
                        .withCondValue(
                            nameQuestion.hasLastNameValue(),
                            nameQuestion.getLastNameValue().orElse(""))
                        .withName(nameQuestion.getLastNamePath().path()),
                    fieldErrors(nameQuestion.getLastNameErrors())));
  }
}
