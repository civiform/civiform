package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NumberQuestion;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class NumberQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public NumberQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    NumberQuestion numberQuestion = question.createNumberQuestion();

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
            input()
                .withType("number")
                .withCondValue(
                    numberQuestion.getNumberValue().isPresent(),
                    numberQuestion.getNumberValue().map(String::valueOf).orElse(""))
                .withName(numberQuestion.getNumberPath().path()),
            fieldErrors(numberQuestion.getQuestionErrors()));
  }
}
