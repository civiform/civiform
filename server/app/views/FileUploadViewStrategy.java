package views;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.footer;
import static j2html.TagCreator.form;
import static j2html.attributes.Attr.ENCTYPE;
import static j2html.attributes.Attr.FORM;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import controllers.applicant.routes;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http.HttpVerbs;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.cloud.FileNameFormatter;
import services.cloud.StorageUploadRequest;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.FileUploadQuestionRenderer;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * A strategy pattern that abstracts out the logic of file upload/download into the different cloud
 * providers.
 */
public abstract class FileUploadViewStrategy extends ApplicationBaseView {

  static final String MIME_TYPES_IMAGES_AND_PDF = "image/*,.pdf";
  final String BLOCK_FORM_ID = "cf-block-form";
  final String FILEUPLOAD_CONTINUE_FORM_ID = "cf-fileupload-continue-form";
  final String FILEUPLOAD_DELETE_FORM_ID = "cf-fileupload-delete-form";
  final String FILEUPLOAD_SUBMIT_FORM_ID = "cf-block-submit";
  final String FILEUPLOAD_DELETE_BUTTON_ID = "fileupload-delete-button";
  final String FILEUPLOAD_SKIP_BUTTON_ID = "fileupload-skip-button";
  final String FILEUPLOAD_CONTINUE_BUTTON_ID = "fileupload-continue-button";

  /**
   * Method to generate the field tags for the file upload view form.
   *
   * @param params the fields necessary to render applicant questions.
   * @param fileUploadQuestion The question that requires a file upload.
   * @return a container tag with the necessary fields
   */
  public abstract ContainerTag signedFileUploadFields(
      ApplicantQuestionRendererParams params, FileUploadQuestion fileUploadQuestion);

  /**
   * Method to render the UI for uploading a file.
   *
   * @param params the information needed to render a file upload view
   * @param applicantQuestionRendererFactory a class for rendering applicant questions.
   * @return a container tag with the submit view
   */
  public final Tag renderFileUploadBlock(
      Params params, ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    String onSuccessRedirectUrl =
        params.baseUrl()
            + routes.ApplicantProgramBlocksController.updateFile(
                    params.applicantId(),
                    params.programId(),
                    params.block().getId(),
                    params.inReview())
                .url();

    String key = FileNameFormatter.formatFileUploadQuestionFilename(params);
    StorageUploadRequest signedRequest =
        params.storageClient().getSignedUploadRequest(key, onSuccessRedirectUrl);

    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setSignedFileUploadRequest(signedRequest)
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    ContainerTag uploadForm = renderFileUploadFormElement(params, signedRequest);
    Preconditions.checkState("form".equals(uploadForm.getTagName()), "must be of type form");
    uploadForm.with(
        each(
            params.block().getQuestions(),
            question ->
                applicantQuestionRendererFactory.getRenderer(question).render(rendererParams)));

    Tag skipForms = renderDeleteAndContinueFileUploadForms(params);
    Tag buttons = renderFileUploadBottomNavButtons(params);

    return div(uploadForm, skipForms, buttons).with(each(extraScriptTags(), tag -> footer(tag)));
  }

  protected ContainerTag renderFileUploadFormElement(Params params, StorageUploadRequest request) {
    return form()
        .withId(BLOCK_FORM_ID)
        .attr(ENCTYPE, "multipart/form-data")
        .withMethod(HttpVerbs.POST);
  }

  protected ImmutableList<Tag> extraScriptTags() {
    return ImmutableList.of();
  }

  /**
   * Renders a form submit button for delete form if the file upload question is optional.
   *
   * <p>If an uploaded file is present, render the button text as delete. Otherwise, skip.
   *
   * <p>See {@link renderDeleteAndContinueFileUploadForms}.
   */
  private Optional<ContainerTag> maybeRenderSkipOrDeleteButton(Params params) {
    if (hasAtLeastOneRequiredQuestion(params)) {
      // If the file question is required, skip or delete is not allowed.
      return Optional.empty();
    }
    String buttonText = params.messages().at(MessageKey.BUTTON_SKIP_FILEUPLOAD.getKeyName());
    String buttonId = FILEUPLOAD_SKIP_BUTTON_ID;
    if (hasUploadedFile(params)) {
      buttonText = params.messages().at(MessageKey.BUTTON_DELETE_FILE.getKeyName());
      buttonId = FILEUPLOAD_DELETE_BUTTON_ID;
    }
    ContainerTag button =
        TagCreator.button(buttonText)
            .withType("submit")
            .attr(FORM, FILEUPLOAD_DELETE_FORM_ID)
            .withClasses(ApplicantStyles.BUTTON_REVIEW)
            .withId(buttonId);
    return Optional.of(button);
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
            .withId(FILEUPLOAD_CONTINUE_BUTTON_ID);
    return Optional.of(button);
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
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

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

  private Tag renderUploadButton(Params params) {
    String styles = ApplicantStyles.BUTTON_BLOCK_NEXT;
    if (hasUploadedFile(params)) {
      styles = ApplicantStyles.BUTTON_REVIEW;
    }
    return submitButton(params.messages().at(MessageKey.BUTTON_UPLOAD.getKeyName()))
        .attr(FORM, BLOCK_FORM_ID)
        .withClasses(styles)
        .withId(FILEUPLOAD_SUBMIT_FORM_ID);
  }

  private Tag renderFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, false);
  }

  private Tag renderEmptyFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, true);
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

  protected String acceptFileTypes() {
    return MIME_TYPES_IMAGES_AND_PDF;
  }

  protected ContainerTag errorDiv(Messages messages, FileUploadQuestion fileUploadQuestion) {
    return div(fileUploadQuestion.fileRequiredMessage().getMessage(messages))
        .withClasses(
            ReferenceClasses.FILEUPLOAD_ERROR, BaseStyles.FORM_ERROR_TEXT_BASE, Styles.HIDDEN);
  }

  private Tag renderFileUploadBottomNavButtons(Params params) {
    Optional<Tag> maybeContinueButton = maybeRenderContinueButton(params);
    Optional<ContainerTag> maybeSkipOrDeleteButton = maybeRenderSkipOrDeleteButton(params);
    ContainerTag ret =
        div()
            .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
            // An empty div to take up the space to the left of the buttons.
            .with(div().withClasses(Styles.FLEX_GROW))
            .with(renderReviewButton(params))
            .with(renderPreviousButton(params));
    if (maybeSkipOrDeleteButton.isPresent()) {
      ret.with(maybeSkipOrDeleteButton.get());
    }
    ret.with(renderUploadButton(params));
    if (maybeContinueButton.isPresent()) {
      ret.with(maybeContinueButton.get());
    }
    return ret;
  }
}
