package views.admin.questions;

import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import j2html.tags.specialized.DivTag;
import play.i18n.Messages;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;
import views.applicant.ApplicantFileUploadRenderer;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;

/** Contains methods for rendering preview of a question. */
public final class QuestionPreview {

  private static DivTag buildQuestionRenderer(
      QuestionType type, Messages messages, ApplicantFileUploadRenderer applicantFileUploadRenderer)
      throws UnsupportedQuestionTypeException {
    ApplicantQuestionRendererFactory rf =
        new ApplicantQuestionRendererFactory(applicantFileUploadRenderer);
    ApplicantQuestionRendererParams params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .build();
    return div(rf.getSampleRenderer(type).render(params));
  }

  public static DivTag renderQuestionPreview(
      QuestionType type,
      Messages messages,
      ApplicantFileUploadRenderer applicantFileUploadRenderer) {
    DivTag titleContainer =
        div()
            .withId("sample-render")
            .withClasses("text-gray-800", "font-thin", "text-xl", "mx-auto", "w-max", "my-4")
            .withText("Sample Question of type: ")
            .with(
                span()
                    .withText(type.getLabel())
                    .withClasses(ReferenceClasses.QUESTION_TYPE, "font-semibold"));

    DivTag renderedQuestion;
    try {
      renderedQuestion = buildQuestionRenderer(type, messages, applicantFileUploadRenderer);
    } catch (UnsupportedQuestionTypeException e) {
      throw new RuntimeException(e);
    }

    DivTag innerContentContainer =
        div(renderedQuestion).withClasses("text-3xl", "pl-16", "pt-20", "w-full");
    DivTag contentContainer = div(innerContentContainer).withId("sample-question");

    return div(titleContainer, contentContainer)
        .withClasses("w-3/5", ApplicantStyles.BODY_BG_COLOR, "overflow-hidden", "overflow-y-auto");
  }
}
