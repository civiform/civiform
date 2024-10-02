package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import com.google.inject.Inject;
import j2html.tags.specialized.DivTag;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.applicant.ApplicantFileUploadRenderer;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;

/** Contains methods for rendering preview of a question. */
public final class QuestionPreview {
  private final SettingsManifest settingsManifest;

  @Inject
  public QuestionPreview(SettingsManifest settingsManifest) {
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  private DivTag buildQuestionRenderer(
      QuestionType type,
      Messages messages,
      ApplicantFileUploadRenderer applicantFileUploadRenderer,
      Request request)
      throws UnsupportedQuestionTypeException {
    ApplicantQuestionRendererFactory rf =
        new ApplicantQuestionRendererFactory(applicantFileUploadRenderer);
    ApplicantQuestionRendererParams params;

    ApplicantQuestionRendererParams.Builder paramsBuilder =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setMultipleFileUploadEnabled(settingsManifest.getMultipleFileUploadEnabled(request))
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS);
    if (type == QuestionType.NAME) {
      params =
          paramsBuilder
              .setIsNameSuffixEnabled(settingsManifest.getNameSuffixDropdownEnabled(request))
              .build();
    } else {
      params = paramsBuilder.build();
    }
    return div(rf.getSampleRenderer(type).render(params));
  }

  public DivTag renderQuestionPreview(
      QuestionType type,
      Messages messages,
      ApplicantFileUploadRenderer applicantFileUploadRenderer,
      Request request) {
    DivTag titleContainer =
        div()
            .withId("sample-render")
            .withClasses("text-gray-800", "font-thin", "text-xl", "mx-auto", "w-max", "my-4")
            .withText("Sample question of type: ")
            .with(
                span()
                    .withText(type.getLabel())
                    .withClasses(ReferenceClasses.QUESTION_TYPE, "font-semibold"));
    DivTag renderedQuestion;
    try {
      renderedQuestion =
          buildQuestionRenderer(type, messages, applicantFileUploadRenderer, request);
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
