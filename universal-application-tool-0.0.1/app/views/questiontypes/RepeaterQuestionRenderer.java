package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class RepeaterQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public RepeaterQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    ApplicantQuestion.RepeaterQuestion repeaterQuestion = question.getRepeaterQuestion();

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
                .withType("text")
                .withCondValue(
                    repeaterQuestion.hasValue(),
                    repeaterQuestion.getRepeatedEntities().map(ImmutableList::toString).orElse(""))
                .withName(repeaterQuestion.getRepeatedEntitiesPath().path()),
            fieldErrors(repeaterQuestion.getQuestionErrors()));
  }
}
