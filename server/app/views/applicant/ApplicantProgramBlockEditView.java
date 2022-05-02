package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.attributes.Attr.HREF;

import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.Block;
import services.applicant.question.ApplicantQuestion;
import services.cloud.StorageClient;
import views.BaseHtmlView;
import views.FileUploadViewStrategy;
import views.HtmlBundle;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.EnumeratorQuestionRenderer;
import views.style.ApplicantStyles;
import views.style.Styles;

/** Renders a page for answering questions in a program screen (block). */
public final class ApplicantProgramBlockEditView extends BaseHtmlView {
  private final String BLOCK_FORM_ID = "cf-block-form";

  private final ApplicantLayout layout;
  private final FileUploadViewStrategy fileUploadStrategy;
  private final ApplicantQuestionRendererFactory applicantQuestionRendererFactory;

  @Inject
  ApplicantProgramBlockEditView(
      ApplicantLayout layout,
      FileUploadViewStrategy fileUploadStrategy,
      @Assisted ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    this.layout = checkNotNull(layout);
    this.fileUploadStrategy = checkNotNull(fileUploadStrategy);
    this.applicantQuestionRendererFactory = applicantQuestionRendererFactory;
  }

  public Content render(Params params) {
    Tag blockDiv =
        div()
            .with(div(renderBlockWithSubmitForm(params)).withClasses(Styles.MY_8))
            .withClasses(Styles.MY_8, Styles.M_AUTO);

    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(params.programTitle())
            .addMainContent(
                h1(params.programTitle()
                        + " "
                        + params.blockIndex()
                        + "/"
                        + params.totalBlockCount())
                    .withClasses(Styles.SR_ONLY))
            .addMainContent(
                layout.renderProgramApplicationTitleAndProgressIndicator(
                    params.programTitle(), params.blockIndex(), params.totalBlockCount(), false),
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

    // Add question validation scripts.
    bundle.addFooterScripts(layout.viewUtils.makeLocalJsTag("validation"));

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
      return fileUploadStrategy.renderFileUploadBlockSubmitForms(
          params, applicantQuestionRendererFactory);
    }
    String formAction =
        routes.ApplicantProgramBlocksController.update(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    return form()
        .withId(BLOCK_FORM_ID)
        .withAction(formAction)
        .withMethod(HttpVerbs.POST)
        .with(makeCsrfTokenInputTag(params.request()))
        .with(
            each(
                params.block().getQuestions(),
                question -> renderQuestion(question, rendererParams)))
        .with(renderBottomNavButtons(params));
  }

  private Tag renderQuestion(ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    checkNotNull(
        applicantQuestionRendererFactory,
        "Must call init function for initializing ApplicantQuestionRendererFactory");
    return applicantQuestionRendererFactory.getRenderer(question).render(params);
  }

  private Tag renderBottomNavButtons(Params params) {
    return div()
        .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
        // An empty div to take up the space to the left of the buttons.
        .with(div().withClasses(Styles.FLEX_GROW))
        .with(renderReviewButton(params))
        .with(renderPreviousButton(params))
        .with(renderNextButton(params));
  }

  private Tag renderReviewButton(Params params) {
    String reviewUrl =
        routes.ApplicantProgramReviewController.review(params.applicantId(), params.programId())
            .url();
    return a().attr(HREF, reviewUrl)
        .withText(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
        .withId("review-application-button")
        .withClasses(ApplicantStyles.BUTTON_REVIEW);
  }

  private Tag renderPreviousButton(Params params) {
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

    return a().attr(HREF, redirectUrl)
        .withText(params.messages().at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName()))
        .withClasses(ApplicantStyles.BUTTON_BLOCK_PREVIOUS)
        .withId("cf-block-previous");
  }

  private Tag renderNextButton(Params params) {
    return submitButton(params.messages().at(MessageKey.BUTTON_NEXT_SCREEN.getKeyName()))
        .withClasses(ApplicantStyles.BUTTON_BLOCK_NEXT)
        .withId("cf-block-submit");
  }

  @AutoValue
  public abstract static class Params {
    public static Builder builder() {
      return new AutoValue_ApplicantProgramBlockEditView_Params.Builder();
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

    public abstract String applicantName();

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

      public abstract Builder setApplicantName(String applicantName);

      public abstract Builder setErrorDisplayMode(
          ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode);

      public abstract Params build();
    }
  }
}
