package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.specialized.DivTag;
import services.applicant.question.ApplicantQuestion;
import views.components.TextFormatter;
import views.style.ReferenceClasses;

/** This renders the question text as formatted text. */
public class StaticContentQuestionRenderer implements ApplicantQuestionRenderer {
  private final ApplicantQuestion question;

  public StaticContentQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-static";
  }

  @Override
  public DivTag render(ApplicantQuestionRendererParams params) {
    DivTag questionTextDiv =
        div()
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, "mb-2", "font-bold", "text-xl")
            .with(
                TextFormatter.formatText(
                    question.getQuestionText(),
                    /*preserveEmptyLines= */ true,
                    /*addRequiredIndicator= */ false));
    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses("mx-auto", "mb-8", this.getReferenceClass())
        .with(questionTextDiv);
  }
}
