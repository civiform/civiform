package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;
import views.style.Styles;

public class TextQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public TextQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    ApplicantQuestion.TextQuestion textQuestion = question.getTextQuestion();

    return div()
        .withId(question.getPath().path())
        .withClasses(Styles.MX_AUTO, Styles.W_MAX)
        .with(
            div().withClasses("applicant-question-text").withText(question.getQuestionText()),
            div()
                .withClasses(
                    "applicant-question-help-text", Styles.TEXT_BASE, Styles.FONT_THIN, Styles.MB_2)
                .withText(question.getQuestionHelpText()),
            input()
                .withType("text")
                .withCondValue(textQuestion.hasValue(), textQuestion.getTextValue().orElse(""))
                .withName(textQuestion.getTextPath().path()),
            fieldErrors(textQuestion.getQuestionErrors()));
  }
}
