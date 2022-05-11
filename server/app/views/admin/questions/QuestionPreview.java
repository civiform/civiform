package views.admin.questions;

import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import j2html.tags.specialized.DivTag;

import play.i18n.Messages;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;
import views.FileUploadViewStrategy;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Contains methods for rendering preview of a question. */
public class QuestionPreview {

  private static ContainerTag buildQuestionRenderer(
      QuestionType type, Messages messages, FileUploadViewStrategy fileUploadViewStrategy)
      throws UnsupportedQuestionTypeException {
    ApplicantQuestionRendererFactory rf =
        new ApplicantQuestionRendererFactory(fileUploadViewStrategy);
    ApplicantQuestionRendererParams params = ApplicantQuestionRendererParams.sample(messages);
    return div(rf.getSampleRenderer(type).render(params));
  }

  public static DivTag renderQuestionPreview(
      QuestionType type, Messages messages, FileUploadViewStrategy fileUploadViewStrategy) {
    DivTag titleContainer =
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
            .with(
                span()
                    .withText(type.toString())
                    .withClasses(ReferenceClasses.QUESTION_TYPE, Styles.FONT_SEMIBOLD));

    DivTag renderedQuestion = div();
    try {
      renderedQuestion = buildQuestionRenderer(type, messages, fileUploadViewStrategy);
    } catch (UnsupportedQuestionTypeException e) {
      renderedQuestion = div().withText(e.toString());
    }
    DivTag innerContentContainer =
        div(renderedQuestion)
            .withClasses(Styles.TEXT_3XL, Styles.PL_16, Styles.PT_20, Styles.W_FULL);
    DivTag contentContainer = div(innerContentContainer).withId("sample-question");

    return div(titleContainer, contentContainer)
        .withClasses(
            Styles.W_3_5,
            ApplicantStyles.BODY_BG_COLOR,
            Styles.OVERFLOW_HIDDEN,
            Styles.OVERFLOW_Y_AUTO);
  }
}
