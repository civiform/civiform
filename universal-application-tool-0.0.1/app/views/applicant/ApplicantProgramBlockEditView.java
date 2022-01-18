package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.attributes.Attr.ENCTYPE;
import static j2html.attributes.Attr.FORM;
import static j2html.attributes.Attr.HREF;

import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.Block;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.cloud.aws.SignedS3UploadRequest;
import services.cloud.aws.SimpleStorage;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.EnumeratorQuestionRenderer;
import views.questiontypes.FileUploadQuestionRenderer;
import views.style.ApplicantStyles;
import views.style.Styles;

/** Renders a page for answering questions in a program screen (block). */
public final class ApplicantProgramBlockEditView extends BaseHtmlView {
  private final String BLOCK_FORM_ID = "cf-block-form";
  private final String FILEUPLOAD_CONTINUE_FORM_ID = "cf-fileupload-continue-form";
  private final String FILEUPLOAD_DELETE_FORM_ID = "cf-fileupload-delete-form";

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
      return renderFileUploadBlockSubmitForms(params);
    }
    String formAction =
        routes.ApplicantProgramBlocksController.update(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder().setMessages(params.messages()).build();

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

  private Tag renderFileUploadBlockSubmitForms(Params params) {
    // Note: This key uniquely identifies the file to be uploaded by the applicant and will be
    // persisted in DB. Other parts of the system rely on the format of the key, e.g. in
    // FileController.java we check if a file can be accessed based on the key content, so be extra
    // cautious if you want to change the format.
    String key =
        String.format(
            "applicant-%d/program-%d/block-%s/${filename}",
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

    Tag uploadForm =
        form()
            .withId(BLOCK_FORM_ID)
            .attr(ENCTYPE, "multipart/form-data")
            .withAction(signedRequest.actionLink())
            .withMethod(HttpVerbs.POST)
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderQuestion(question, rendererParams)));
    Tag skipForms = renderDeleteAndContinueFileUploadForms(params);
    Tag buttons = renderFileUploadBottomNavButtons(params);
    return div(uploadForm, skipForms, buttons);
  }

  /**
   * Returns two hidden forms for navigating through a file upload block without uploading a file.
   *
   * <p>Delete form sends an update with an empty file key. An empty file key erases the existing
   * file key if one is present. In either case, the file upload question is marked as seen but
   * unanswered, namely skipping the file upload. This is only allowed for an optional question.
   *
   * <p>Continue form sends an update with the currently stored file key, the same behavior as an
   * applicant re-submits a form without changing their answer. Continue form is only used when an
   * existing file (and file key) is present.
   */
  private Tag renderDeleteAndContinueFileUploadForms(Params params) {
    String formAction =
        routes.ApplicantProgramBlocksController.update(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder().setMessages(params.messages()).build();

    Tag continueForm =
        form()
            .withId(FILEUPLOAD_CONTINUE_FORM_ID)
            .withAction(formAction)
            .withMethod(HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(params.request()))
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderFileKeyField(question, rendererParams)));
    Tag deleteForm =
        form()
            .withId(FILEUPLOAD_DELETE_FORM_ID)
            .withAction(formAction)
            .withMethod(HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(params.request()))
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderEmptyFileKeyField(question, rendererParams)));
    return div(continueForm, deleteForm).withClasses(Styles.HIDDEN);
  }

  private Tag renderQuestion(ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return applicantQuestionRendererFactory.getRenderer(question).render(params);
  }

  private Tag renderFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, false);
  }

  private Tag renderEmptyFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, true);
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

  private Tag renderFileUploadBottomNavButtons(Params params) {
    Optional<Tag> maybeContinueButton = maybeRenderContinueButton(params);
    Optional<Tag> maybeSkipOrDeleteButton = maybeRenderSkipOrDeleteButton(params);
    ContainerTag ret =
        div()
            .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
            // An empty div to take up the space to the left of the buttons.
            .with(div().withClasses(Styles.FLEX_GROW))
            .with(renderReviewButton(params));
    if (maybeSkipOrDeleteButton.isPresent()) {
      ret.with(maybeSkipOrDeleteButton.get());
    }
    ret.with(renderUploadButton(params));
    if (maybeContinueButton.isPresent()) {
      ret.with(maybeContinueButton.get());
    }
    return ret;
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

  /**
   * Renders a form submit button for continue form if an uploaded file is present.
   *
   * <p>See {@link renderDeleteAndContinueFileUploadForms}.
   */
  private Optional<Tag> maybeRenderContinueButton(Params params) {
    if (!hasUploadedFile(params)) {
      return Optional.empty();
    }
    Tag button =
        submitButton(params.messages().at(MessageKey.BUTTON_KEEP_FILE.getKeyName()))
            .attr(FORM, FILEUPLOAD_CONTINUE_FORM_ID)
            .withClasses(ApplicantStyles.BUTTON_BLOCK_NEXT)
            .withId("fileupload-continue-button");
    return Optional.of(button);
  }

  /**
   * Renders a form submit button for delete form if the file upload question is optional.
   *
   * <p>If an uploaded file is present, render the button text as delete. Otherwise, skip.
   *
   * <p>See {@link renderDeleteAndContinueFileUploadForms}.
   */
  private Optional<Tag> maybeRenderSkipOrDeleteButton(Params params) {
    if (hasAtLeastOneRequiredQuestion(params)) {
      // If the file question is required, skip or delete is not allowed.
      return Optional.empty();
    }
    String buttonText = params.messages().at(MessageKey.BUTTON_SKIP_FILEUPLOAD.getKeyName());
    String buttonId = "fileupload-skip-button";
    if (hasUploadedFile(params)) {
      buttonText = params.messages().at(MessageKey.BUTTON_DELETE_FILE.getKeyName());
      buttonId = "fileupload-delete-button";
    }
    Tag button =
        submitButton(buttonText)
            .attr(FORM, FILEUPLOAD_DELETE_FORM_ID)
            .withClasses(ApplicantStyles.BUTTON_REVIEW)
            .withId(buttonId);
    return Optional.of(button);
  }

  private Tag renderUploadButton(Params params) {
    String styles = ApplicantStyles.BUTTON_BLOCK_NEXT;
    if (hasUploadedFile(params)) {
      styles = ApplicantStyles.BUTTON_REVIEW;
    }
    return submitButton(params.messages().at(MessageKey.BUTTON_UPLOAD.getKeyName()))
        .attr(FORM, BLOCK_FORM_ID)
        .withClasses(styles)
        .withId("cf-block-submit");
  }

  private boolean hasUploadedFile(Params params) {
    return params.block().getQuestions().stream()
        .map(ApplicantQuestion::createFileUploadQuestion)
        .map(FileUploadQuestion::getFileKeyValue)
        .anyMatch(maybeValue -> maybeValue.isPresent());
  }

  private boolean hasAtLeastOneRequiredQuestion(Params params) {
    return params.block().getQuestions().stream().anyMatch(question -> !question.isOptional());
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
