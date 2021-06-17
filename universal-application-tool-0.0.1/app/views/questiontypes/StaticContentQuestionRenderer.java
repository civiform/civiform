package views.questiontypes;

import static j2html.TagCreator.div;

import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.EmailQuestion;
import views.style.ReferenceClasses;
import views.components.FieldWithLabel;
import views.style.ApplicantStyles;
import views.components.TextFormatter;
import views.style.ReferenceClasses;
import views.style.Styles;

/** This renders the question text as formatted text. */
public class StaticContentQuestionRenderer extends ApplicantQuestionRenderer {

  public StaticContentQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "";
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    ContainerTag questionTextDiv =
    div()
        .withClasses(
            ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT)
        .with(
            TextFormatter.formatText(question.getQuestionText(), false)
        );
    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.MB_8, this.getReferenceClass())
        .with(questionTextDiv);
  }
}
