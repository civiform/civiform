package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.select;

import j2html.TagCreator;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class DropdownQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public DropdownQuestionRenderer(ApplicantQuestion question) {
    this.question = question;
  }

  @Override
  public Tag render() {
    SingleSelectQuestion multiOptionQuestion = question.getSingleSelectQuestion();

    return div()
        .withId(question.getPath().toString())
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
            select()
                .with(each(multiOptionQuestion.getOptions(), TagCreator::option))
                .withName(multiOptionQuestion.getSelectionPath().toString()));
  }
}
