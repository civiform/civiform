package views;

import static j2html.TagCreator.a;

import com.google.auto.value.AutoValue;
import controllers.applicant.routes;
import j2html.tags.specialized.ATag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import services.MessageKey;
import services.applicant.Block;
import services.cloud.StorageClient;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;

public class ApplicationBaseView extends BaseHtmlView {
  final String REVIEW_APPLICATION_BUTTON_ID = "review-application-button";

  protected ATag renderReviewButton(ApplicationBaseView.Params params) {
    String reviewUrl =
        routes.ApplicantProgramReviewController.review(params.applicantId(), params.programId())
            .url();
    return a().withHref(reviewUrl)
        .withText(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
        .withId(REVIEW_APPLICATION_BUTTON_ID)
        .withClasses(ApplicantStyles.BUTTON_REVIEW);
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
          routes.ApplicantProgramReviewController.preview(params.applicantId(), params.programId())
              .url();
    }
    return a().withHref(redirectUrl)
        .withText(params.messages().at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName()))
        .withClasses(ApplicantStyles.BUTTON_BLOCK_PREVIOUS)
        .withId("cf-block-previous");
  }

  @AutoValue
  public abstract static class Params {
    public static Builder builder() {
      return new AutoValue_ApplicationBaseView_Params.Builder();
    }

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

    public abstract Optional<String> applicantName();

    public abstract ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode();

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

      public abstract Builder setApplicantName(Optional<String> applicantName);

      abstract Optional<String> applicantName();

      abstract Messages messages();

      abstract Params autoBuild();

      public final Params build() {
        setApplicantName(Optional.of(ApplicantUtils.getApplicantName(applicantName(), messages())));
        return autoBuild();
      }
    }
  }
}
