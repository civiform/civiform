package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import services.applicant.question.ApplicantQuestion;
import views.components.TextFormatter;
import views.style.ReferenceClasses;

/** This renders the question text as formatted text. */
public class MapQuestionRenderer implements ApplicantQuestionRenderer {
  private final ApplicantQuestion question;

  public MapQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-map";
  }

  @Override
  public DivTag render(ApplicantQuestionRendererParams params) {

    ImmutableList<DomContent> formattedText;
    // Static questions that are shown to the applicant will have the messages object passed in
    // Previews of map questions that are shown to admin will not
    formattedText =
        TextFormatter.formatText(
            question.getQuestionText(),
            /* preserveEmptyLines= */ true,
            /* addRequiredIndicator= */ false,
            /* ariaLabelForNewTabs= */ "opens in a new tab");

    DivTag questionTextDiv =
        div()
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, "mb-2", "font-bold", "text-xl")
            .with(formattedText);
    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses("mx-auto", "mb-8", this.getReferenceClass())
        .with(questionTextDiv);
  }
}
