package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Http.HttpVerbs;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.cloud.ApplicantFileNameFormatter;
import services.cloud.StorageUploadRequest;
import views.ApplicationBaseView;
import views.components.ButtonStyles;
import views.fileupload.FileUploadViewStrategy;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.FileUploadQuestionRenderer;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

/** A helper class for rendering the file upload question for applicants. */
public final class ApplicantFileUploadRenderer extends ApplicationBaseView {

  private static final String MIME_TYPES_IMAGES_AND_PDF = "image/*,.pdf";
  private static final String BLOCK_FORM_ID = "cf-block-form";
  private static final String FILEUPLOAD_CONTINUE_FORM_ID = "cf-fileupload-continue-form";
  private static final String FILEUPLOAD_DELETE_FORM_ID = "cf-fileupload-delete-form";
  private static final String FILEUPLOAD_SUBMIT_FORM_ID = "cf-block-submit";
  private static final String FILEUPLOAD_DELETE_BUTTON_ID = "fileupload-delete-button";
  private static final String FILEUPLOAD_SKIP_BUTTON_ID = "fileupload-skip-button";
  private static final String FILEUPLOAD_CONTINUE_BUTTON_ID = "fileupload-continue-button";

  private final FileUploadViewStrategy fileUploadViewStrategy;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ApplicantFileUploadRenderer(
      FileUploadViewStrategy fileUploadViewStrategy, ApplicantRoutes applicantRoutes) {
    this.fileUploadViewStrategy = checkNotNull(fileUploadViewStrategy);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  /**
   * Method to generate the field tags for the file upload view form.
   *
   * @param params the fields necessary to render applicant questions.
   * @param fileUploadQuestion The question that requires a file upload.
   * @param ariaDescribedByIds HTML tag IDs that this file upload input should be associated with.
   * @param hasErrors whether this file upload input is displaying errors.
   * @return a container tag with the necessary fields
   */
  public DivTag signedFileUploadFields(
      ApplicantQuestionRendererParams params,
      FileUploadQuestion fileUploadQuestion,
      String fileInputId,
      ImmutableList<String> ariaDescribedByIds,
      boolean hasErrors) {
    Optional<String> uploaded =
        fileUploadQuestion
            .getFilename()
            .map(f -> params.messages().at(MessageKey.INPUT_FILE_ALREADY_UPLOADED.getKeyName(), f));

    DivTag result =
        div()
            .with(
                div()
                    .withText(uploaded.orElse(""))
                    // adds INPUT_FILE_ALREADY_UPLOADED text to data attribute here so client side
                    // can render the translated text if it gets added
                    .attr(
                        "data-upload-text",
                        params.messages().at(MessageKey.INPUT_FILE_ALREADY_UPLOADED.getKeyName()))
                    .attr("aria-live", "polite"));
    result.with(
        fileUploadViewStrategy.additionalFileUploadFormInputs(params.signedFileUploadRequest()));
    result.with(createFileInputFormElement(fileInputId, ariaDescribedByIds, hasErrors));
    result.with(
        div(fileUploadQuestion.fileRequiredMessage().getMessage(params.messages()))
            .withId(fileInputId + "-required-error")
            .withClasses(
                ReferenceClasses.FILEUPLOAD_ERROR, BaseStyles.FORM_ERROR_TEXT_BASE, "hidden"));
    result.with(
        p(params.messages().at(MessageKey.MOBILE_FILE_UPLOAD_HELP.getKeyName()))
            .withClasses("text-sm", "text-gray-600", "mb-2"));
    return result;
  }

  /**
   * Method to render the UI for uploading a file.
   *
   * @param params the information needed to render a file upload view
   * @param applicantQuestionRendererFactory a class for rendering applicant questions.
   * @return a container tag with the submit view
   */
  public DivTag renderFileUploadBlock(
      Params params, ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    String onSuccessRedirectUrl =
        params.baseUrl()
            + applicantRoutes
                .updateFile(
                    params.profile(),
                    params.applicantId(),
                    params.programId(),
                    params.block().getId(),
                    params.inReview())
                .url();

    String key =
        ApplicantFileNameFormatter.formatFileUploadQuestionFilename(
            params.applicantId(), params.programId(), params.block().getId());
    StorageUploadRequest signedRequest =
        params.applicantStorageClient().getSignedUploadRequest(key, onSuccessRedirectUrl);

    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setSignedFileUploadRequest(signedRequest)
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    FormTag uploadForm =
        fileUploadViewStrategy
            .renderFileUploadFormElement(signedRequest)
            .withId(BLOCK_FORM_ID)
            .with(requiredFieldsExplanationContent(params.messages()));
    Preconditions.checkState("form".equals(uploadForm.getTagName()), "must be of type form");
    uploadForm.with(
        each(
            params.block().getQuestions(),
            question ->
                applicantQuestionRendererFactory
                    .getRenderer(question, Optional.of(params.messages()))
                    .render(rendererParams)));

    DivTag skipForms = renderDeleteAndContinueFileUploadForms(params);
    DivTag buttons = renderFileUploadBottomNavButtons(params);

    return div(uploadForm, skipForms, buttons).with(fileUploadViewStrategy.footerTags());
  }

  /**
   * Creates the <input type="file"> element needed for the file upload <form>.
   *
   * <p>Note: This likely could be migrated to use the USWDS file input component instead -- see
   * {@link FileUploadViewStrategy#createUswdsFileInputFormElement}.
   *
   * @param fileInputId an ID associated with the file <input> field. Can be used to associate
   *     custom screen reader functionality with the file input.
   */
  private InputTag createFileInputFormElement(
      String fileInputId, ImmutableList<String> ariaDescribedByIds, boolean hasErrors) {
    return input()
        .withId(fileInputId)
        .condAttr(hasErrors, "aria-invalid", "true")
        .condAttr(
            !ariaDescribedByIds.isEmpty(),
            "aria-describedby",
            StringUtils.join(ariaDescribedByIds, " "))
        .withType("file")
        .withName("file")
        .withClass("hidden")
        .withAccept(MIME_TYPES_IMAGES_AND_PDF);
  }

  /**
   * Renders a form submit button for delete form if the file upload question is optional.
   *
   * <p>If an uploaded file is present, render the button text as delete. Otherwise, skip.
   *
   * <p>See {@link #renderDeleteAndContinueFileUploadForms}.
   */
  private Optional<ButtonTag> maybeRenderSkipOrDeleteButton(Params params) {
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
    ButtonTag button =
        TagCreator.button(buttonText)
            .withType("submit")
            .withForm(FILEUPLOAD_DELETE_FORM_ID)
            .withClasses(ButtonStyles.OUTLINED_TRANSPARENT)
            .withId(buttonId);
    return Optional.of(button);
  }

  /**
   * Renders a form submit button for continue form if an uploaded file is present.
   *
   * <p>See {@link #renderDeleteAndContinueFileUploadForms}.
   */
  private Optional<ButtonTag> maybeRenderContinueButton(Params params) {
    if (!hasUploadedFile(params)) {
      return Optional.empty();
    }
    ButtonTag button =
        submitButton(params.messages().at(MessageKey.BUTTON_KEEP_FILE.getKeyName()))
            .withForm(FILEUPLOAD_CONTINUE_FORM_ID)
            .withClasses(ButtonStyles.SOLID_BLUE)
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
  private DivTag renderDeleteAndContinueFileUploadForms(Params params) {
    String formAction =
        applicantRoutes
            .updateBlock(
                params.profile(),
                params.applicantId(),
                params.programId(),
                params.block().getId(),
                params.inReview(),
                ApplicantRequestedAction.NEXT_BLOCK)
            .url();
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    FormTag continueForm =
        form()
            .withId(FILEUPLOAD_CONTINUE_FORM_ID)
            .withAction(formAction)
            .withMethod(HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(params.request()))
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderFileKeyField(question, rendererParams)));
    FormTag deleteForm =
        form()
            .withId(FILEUPLOAD_DELETE_FORM_ID)
            .withAction(formAction)
            .withMethod(HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(params.request()))
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderEmptyFileKeyField(question, rendererParams)));
    return div(continueForm, deleteForm).withClasses("hidden");
  }

  private ButtonTag renderNextButton(Params params) {
    String styles = ButtonStyles.SOLID_BLUE;
    if (hasUploadedFile(params)) {
      styles = ButtonStyles.OUTLINED_TRANSPARENT;
    }
    return submitButton(params.messages().at(MessageKey.BUTTON_NEXT_SCREEN.getKeyName()))
        .withForm(BLOCK_FORM_ID)
        .withClasses(styles)
        .withId(FILEUPLOAD_SUBMIT_FORM_ID);
  }

  private DivTag renderFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, false);
  }

  private DivTag renderEmptyFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, true);
  }

  private boolean hasUploadedFile(Params params) {
    return params.block().getQuestions().stream()
        .map(ApplicantQuestion::createFileUploadQuestion)
        .map(FileUploadQuestion::getFileKeyValue)
        .anyMatch(Optional::isPresent);
  }

  private boolean hasAtLeastOneRequiredQuestion(Params params) {
    return params.block().getQuestions().stream().anyMatch(question -> !question.isOptional());
  }

  private DivTag renderFileUploadBottomNavButtons(Params params) {
    Optional<ButtonTag> maybeContinueButton = maybeRenderContinueButton(params);
    Optional<ButtonTag> maybeSkipOrDeleteButton = maybeRenderSkipOrDeleteButton(params);
    DivTag ret =
        div()
            .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
            // TODO(#6450): Use the new review button here.
            .with(renderOldReviewButton(params))
            .with(renderPreviousButton(params));
    if (maybeSkipOrDeleteButton.isPresent()) {
      ret.with(maybeSkipOrDeleteButton.get());
    }
    ret.with(renderNextButton(params));
    if (maybeContinueButton.isPresent()) {
      ret.with(maybeContinueButton.get());
    }
    return ret;
  }
}
