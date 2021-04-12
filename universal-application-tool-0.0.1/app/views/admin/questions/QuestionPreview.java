package views.admin.questions;

import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import j2html.tags.ContainerTag;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.style.Styles;

public class QuestionPreview {

  private static ContainerTag buildQuestionRenderer(QuestionType type)
      throws UnsupportedQuestionTypeException {
    ApplicantQuestionRendererFactory rf = new ApplicantQuestionRendererFactory();
    return div(rf.getSampleRenderer(type).render());
  }

  public static ContainerTag renderQuestionPreview(QuestionType type) {
    ContainerTag titleContainer =
        div()
            .withId("sample-render")
            .withClasses(
                Styles.TEXT_GRAY_800,
                Styles.FONT_THIN,
                Styles.TEXT_XL,
                Styles.MX_AUTO,
                Styles.W_MAX,
                Styles.MY_4)
            .withText("Sample Question of type: ")
            .with(span().withText(type.toString()).withClasses(Styles.FONT_SEMIBOLD));

    ContainerTag renderedQuestion = div();
    try {
      renderedQuestion = buildQuestionRenderer(type);
    } catch (UnsupportedQuestionTypeException e) {
      renderedQuestion = div().withText(e.toString());
    }
    ContainerTag innerContentContainer =
        div(renderedQuestion)
            .withClasses(Styles.TEXT_3XL, Styles.PL_16, Styles.PT_20, Styles.W_FULL);
    ContainerTag contentContainer = div(innerContentContainer).withId("sample-question");

    return div(titleContainer, contentContainer)
        .withClasses(
            Styles.BG_GRADIENT_TO_BR, Styles.FROM_BLUE_200, Styles.TO_BLUE_400, Styles.W_3_5);
  }
}
