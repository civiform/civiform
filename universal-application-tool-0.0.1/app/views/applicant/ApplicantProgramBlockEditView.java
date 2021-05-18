package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.attributes.Attr.ENCTYPE;
import static j2html.attributes.Attr.HREF;

import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.Block;
import services.applicant.question.ApplicantQuestion;
import services.aws.SignedS3UploadRequest;
import services.aws.SimpleStorage;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.EnumeratorQuestionRenderer;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

public final class ApplicantProgramBlockEditView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final ApplicantQuestionRendererFactory applicantQuestionRendererFactory;

  @Inject
  public ApplicantProgramBlockEditView(
      ApplicantLayout layout, ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    this.layout = checkNotNull(layout);
    this.applicantQuestionRendererFactory = checkNotNull(applicantQuestionRendererFactory);
  }

  public Content render(Params params) {
    Tag blockDiv =
        div()
            .with(div(renderBlockWithSubmitForm(params)).withClasses(Styles.MY_8))
            .withClasses(Styles.MY_8, Styles.M_AUTO);

    String reviewUrl =
        routes.ApplicantProgramReviewController.review(params.applicantId(), params.programId())
            .url();
    Tag gotoReviewButton =
        a().attr(HREF, reviewUrl)
            .withText(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
            .withId("review-application-button")
            .withClasses(
                ApplicantStyles.BUTTON_REVIEW);

    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(params.programTitle())
            .addMainContent(
                layout.renderProgramApplicationTitleAndProgressIndicator(
                    params.programTitle(), params.blockIndex(), params.totalBlockCount(), false),
                gotoReviewButton,
                blockDiv)
            .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION);

    if (!params.preferredLanguageSupported()) {
      bundle.addMainContent(
          renderLocaleNotSupportedToast(
              params.applicantId(), params.programId(), params.messages()));
    }

    // Add the hidden enumerator field template
    if (params.block().isEnumerator()) {
      bundle.addMainContent(
          EnumeratorQuestionRenderer.newEnumeratorFieldTemplate(
              params.block().getEnumeratorQuestion().getContextualizedPath(),
              params.block().getEnumeratorQuestion().createEnumeratorQuestion().getEntityType(),
              params.messages()));
    }

    return layout.renderWithNav(
        params.request(), params.applicantName(), params.messages(), bundle);
  }

  /**
   * If the applicant's preferred language is not supported for this program, render a toast
   * warning. Allow them to dismiss the warning, and once it is dismissed it does not reappear for
   * the same program.
   */
  private ContainerTag renderLocaleNotSupportedToast(
      long applicantId, long programId, Messages messages) {
    // Note: we include applicantId and programId in the ID, so that the applicant sees the warning
    // for each program that is not properly localized. Otherwise, once dismissed, this toast would
    // never appear for other programs. Additionally, including the applicantId ensures that this
    // warning still appears across applicants, so that (for example) a Trusted Intermediary
    // handling multiple applicants will see the toast displayed.
    return ToastMessage.warning(messages.at(MessageKey.TOAST_LOCALE_NOT_SUPPORTED.getKeyName()))
        .setId(String.format("locale-not-supported-%d-%d", applicantId, programId))
        .setDismissible(true)
        .setIgnorable(true)
        .setDuration(0)
        .getContainerTag();
  }

  private Tag renderBlockWithSubmitForm(Params params) {
    if (params.block().isFileUpload()) {
      return renderFileUploadBlockSubmitForm(params);
    }
    String formAction =
        routes.ApplicantProgramBlocksController.update(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder().setMessages(params.messages()).build();
    return form()
        .withAction(formAction)
        .withMethod(HttpVerbs.POST)
        .with(makeCsrfTokenInputTag(params.request()))
        .with(
            each(
                params.block().getQuestions(),
                question -> renderQuestion(question, rendererParams)))
        .with(
            submitButton(params.messages().at(MessageKey.BUTTON_NEXT_BLOCK.getKeyName()))
                .withClasses(ApplicantStyles.BUTTON_BLOCK_NEXT));
  }

  private Tag renderFileUploadBlockSubmitForm(Params params) {
    String key =
        String.format(
            "applicant-%d/program-%d/block-%s",
            params.applicantId(), params.programId(), params.block().getId());
    String onSuccessRedirectUrl =
        params.baseUrl()
            + routes.ApplicantProgramBlocksController.updateFile(
                    params.applicantId(),
                    params.programId(),
                    params.block().getId(),
                    params.inReview())
                .url();
    SignedS3UploadRequest signedRequest =
        params.amazonS3Client().getSignedUploadRequest(key, onSuccessRedirectUrl);
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setSignedFileUploadRequest(signedRequest)
            .build();
    return form()
        .attr(ENCTYPE, "multipart/form-data")
        .withAction(signedRequest.actionLink())
        .withMethod(HttpVerbs.POST)
        .with(
            each(
                params.block().getQuestions(),
                question -> renderQuestion(question, rendererParams)))
        .with(submitButton(params.messages().at(MessageKey.BUTTON_NEXT_BLOCK.getKeyName())));
  }

  private Tag renderQuestion(ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return applicantQuestionRendererFactory.getRenderer(question).render(params);
  }

  @AutoValue
  public abstract static class Params {
    public static Builder builder() {
      return new AutoValue_ApplicantProgramBlockEditView_Params.Builder();
    }

    abstract boolean inReview();

    abstract Http.Request request();

    abstract Messages messages();

    abstract int blockIndex();

    abstract int totalBlockCount();

    abstract long applicantId();

    abstract String programTitle();

    abstract long programId();

    abstract Block block();

    abstract boolean preferredLanguageSupported();

    abstract SimpleStorage amazonS3Client();

    abstract String baseUrl();

    abstract String applicantName();

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

      public abstract Builder setAmazonS3Client(SimpleStorage amazonS3Client);

      public abstract Builder setBaseUrl(String baseUrl);

      public abstract Builder setApplicantName(String applicantName);

      public abstract Params build();
    }
  }
}
