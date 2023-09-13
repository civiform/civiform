package views;

import static j2html.TagCreator.a;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;

import com.google.auto.value.AutoValue;
import controllers.applicant.routes;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.SpanTag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.Block;
import services.cloud.StorageClient;
import views.components.ButtonStyles;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererParams;

public class ApplicationBaseView extends BaseHtmlView {
  final String REVIEW_APPLICATION_BUTTON_ID = "review-application-button";

  protected ATag renderReviewButton(ApplicationBaseView.Params params) {
    String reviewUrl =
        routes.ApplicantProgramReviewController.review(params.applicantId(), params.programId())
            .url();
    return a().withHref(reviewUrl)
        .withText(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
        .withId(REVIEW_APPLICATION_BUTTON_ID)
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT);
  }

  protected ATag renderPreviousButton(ApplicationBaseView.Params params) {
    int previousBlockIndex = params.blockIndex() - 1;
    String redirectUrl;

    if (previousBlockIndex >= 0) {
      redirectUrl =
          routes.ApplicantProgramBlocksController.previous(
                  params.applicantId(), params.programId(), previousBlockIndex, params.inReview())
              .url();
    } else {
      redirectUrl =
          routes.ApplicantProgramReviewController.review(params.applicantId(), params.programId())
              .url();
    }
    return a().withHref(redirectUrl)
        .withText(params.messages().at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName()))
        .withClasses(ButtonStyles.SOLID_BLUE)
        .withId("cf-block-previous");
  }

  @AutoValue
  public abstract static class Params {
    public static Builder builder() {
      return new AutoValue_ApplicationBaseView_Params.Builder();
    }

    public abstract Builder toBuilder();

    public abstract boolean inReview();

    public abstract Http.Request request();

    public abstract Messages messages();

    public abstract int blockIndex();

    public abstract int totalBlockCount();

    public abstract long applicantId();

    public abstract String programTitle();

    public abstract long programId();

    public abstract Block block();

    public abstract boolean preferredLanguageSupported();

    public abstract StorageClient storageClient();

    public abstract String baseUrl();

    public abstract ApplicantPersonalInfo applicantPersonalInfo();

    public abstract ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode();

    public abstract Optional<ToastMessage> bannerMessage();

    public abstract Optional<String> questionName();

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setRequest(Http.Request request);

      public abstract Builder setInReview(boolean inReview);

      public abstract Builder setMessages(Messages messages);

      public abstract Builder setBlockIndex(int blockIndex);

      public abstract Builder setTotalBlockCount(int blockIndex);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setProgramTitle(String programTitle);

      public abstract Builder setProgramId(long programId);

      public abstract Builder setBlock(Block block);

      public abstract Builder setPreferredLanguageSupported(boolean preferredLanguageSupported);

      public abstract Builder setStorageClient(StorageClient storageClient);

      public abstract Builder setBaseUrl(String baseUrl);

      public abstract Builder setErrorDisplayMode(
          ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode);

      public abstract Builder setBannerMessage(Optional<ToastMessage> banner);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo personalInfo);

      public abstract Builder setQuestionName(Optional<String> questionName);

      public abstract Params build();
    }
  }

  /**
   * Renders "Note: Fields marked with a * are required."
   *
   * @param messages the localized {@link Messages} for the current applicant
   * @return PTag containing requiredness text.
   */
  public static PTag requiredFieldsExplanationContent(Messages messages) {
    SpanTag redAsterisk = span("*").withClass("text-red-700");
    return p(rawHtml(messages.at(MessageKey.REQUIRED_FIELDS_NOTE.getKeyName(), redAsterisk)))
        .withClasses("text-sm", "text-gray-600", "mb-2");
  }
}
