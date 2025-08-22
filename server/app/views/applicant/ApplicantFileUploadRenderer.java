package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.applicant.ApplicantRequestedAction.NEXT_BLOCK;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ul;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.nimbusds.jose.shaded.gson.JsonArray;
import controllers.applicant.ApplicantRoutes;
import j2html.TagCreator;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.UlTag;
import java.util.Optional;
import java.util.OptionalInt;
import javax.inject.Inject;
import play.mvc.Http.HttpVerbs;
import services.AlertType;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.cloud.ApplicantFileNameFormatter;
import services.cloud.ApplicantStorageClient;
import services.cloud.StorageUploadRequest;
import services.settings.SettingsManifest;
import views.AlertComponent;
import views.ApplicationBaseView;
import views.ApplicationBaseViewParams;
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

  private static final String ALLOWED_FILE_TYPE_SPECIFIERS_FALLBACK = "image/*,.pdf";
  private static final String BLOCK_FORM_ID = "cf-block-form";
  private static final String FILEUPLOAD_CONTINUE_FORM_ID = "cf-fileupload-continue-form";
  private static final String FILEUPLOAD_DELETE_FORM_ID = "cf-fileupload-delete-form";
  private static final String FILE_UPLOADING_TAG_CLASS = "cf-file-uploading-tag";

  // Class to apply to elements which should be disabled when an upload is triggered. This logic is
  // implemented in file_upload.ts
  private static final String DISABLED_WHEN_UPLOADING_CLASS = "cf-disable-when-uploading";

  private final FileUploadViewStrategy fileUploadViewStrategy;
  private final ApplicantRoutes applicantRoutes;
  private final ApplicantStorageClient applicantStorageClient;
  private final SettingsManifest settingsManifest;

  @Inject
  public ApplicantFileUploadRenderer(
      FileUploadViewStrategy fileUploadViewStrategy,
      ApplicantRoutes applicantRoutes,
      ApplicantStorageClient applicantStorageClient,
      SettingsManifest settingsManifest) {
    this.fileUploadViewStrategy = checkNotNull(fileUploadViewStrategy);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.applicantStorageClient = checkNotNull(applicantStorageClient);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /**
   * Method to generate ul which contains a list of all uploaded files.
   *
   * @param fileUploadQuestion The question which files were uploaded for
   * @return ul containing list of all uploaded files
   */
  public UlTag uploadedFiles(
      ApplicationBaseViewParams params, FileUploadQuestion fileUploadQuestion) {
    UlTag result =
        ul().attr("aria-label", params.messages().at(MessageKey.UPLOADED_FILES.getKeyName()));

    JsonArray uploadedFileNames = new JsonArray();

    if (fileUploadQuestion.getFileKeyListValue().isPresent()) {
      int i = 0;
      for (String fileKey : fileUploadQuestion.getFileKeyListValue().get()) {
        i++;
        String fileNameId = "uploaded-file-" + i;
        String fileName = FileUploadQuestion.getFileName(fileKey);

        uploadedFileNames.add(fileName);

        String removeUrl =
            applicantRoutes
                .removeFile(
                    params.profile(),
                    params.applicantId(),
                    params.programId(),
                    params.block().getId(),
                    fileKey,
                    params.inReview())
                .url();

        result.with(
            li().withClass("flex justify-between mb-2")
                .withId(fileNameId)
                .with(TagCreator.span().withText(fileName).withClasses("overflow-x-hidden"))
                .with(
                    TagCreator.a()
                        .withText(params.messages().at(MessageKey.LINK_REMOVE_FILE.getKeyName()))
                        .withHref(removeUrl)
                        .attr("aria-labelledby", fileNameId)
                        .withClasses(
                            BaseStyles.LINK_TEXT,
                            BaseStyles.LINK_HOVER_TEXT,
                            "underline",
                            "ml-2",
                            "shrink-0")));
      }
    }

    // Add 'uploaded-files' attribute, which is used by file_upload.ts
    return result.withData("uploaded-files", uploadedFileNames.toString());
  }

  private String getFileInputHint(
      FileUploadQuestion question, ApplicantQuestionRendererParams params) {
    OptionalInt maxFiles = question.getQuestionDefinition().getMaxFiles();

    if (maxFiles.isPresent()) {
      if (maxFiles.getAsInt() == 1) {
        return params.messages().at(MessageKey.INPUT_SINGLE_FILE_UPLOAD_HINT.getKeyName());
      } else {
        return params
            .messages()
            .at(MessageKey.INPUT_MULTIPLE_FILE_UPLOAD_HINT.getKeyName(), maxFiles.getAsInt());
      }
    }
    return params.messages().at(MessageKey.INPUT_UNLIMITED_FILE_UPLOAD_HINT.getKeyName());
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
    DivTag result = div();
    result.with(
        fileUploadViewStrategy.additionalFileUploadFormInputs(params.signedFileUploadRequest()));

    result.with(
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            fileInputId,
            settingsManifest
                .getFileUploadAllowedFileTypeSpecifiers()
                .orElse(ALLOWED_FILE_TYPE_SPECIFIERS_FALLBACK),
            ImmutableList.of(getFileInputHint(fileUploadQuestion, params)),
            /* disabled= */ !fileUploadQuestion.canUploadFile(),
            applicantStorageClient.getFileLimitMb(),
            params.messages()));
    // TODO(#6804): Use HTMX to add these errors to the DOM only when they're needed.
    result.with(
        AlertComponent.renderSlimAlert(
                AlertType.ERROR,
                fileUploadQuestion.fileRequiredMessage().getMessage(params.messages()),
                // file_upload.ts will un-hide this error if needed.
                /* hidden= */ true,
                "mb-2")
            .withId(ReferenceClasses.FILEUPLOAD_REQUIRED_ERROR_ID));

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
      ApplicationBaseViewParams params,
      ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {

    String onSuccessRedirectUrl;
    onSuccessRedirectUrl =
        params.baseUrl()
            + applicantRoutes
                .addFile(
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

    Optional<ApplicantQuestion> fileUploadQuestion =
        params.block().getQuestions().stream()
            .filter(question -> question.isFileUploadQuestion())
            .findFirst();

    Preconditions.checkState(
        fileUploadQuestion.isPresent(), "File upload blocks must contain a file upload question.");
    uploadForm.withClasses("max-w-xl", fileUploadViewStrategy.getMultiFileUploadFormClass());
    uploadForm.with(
        applicantQuestionRendererFactory
            .getRenderer(fileUploadQuestion.get(), Optional.of(params.messages()))
            .render(rendererParams)
            .with(createFileUploadTag(params))
            .with(uploadedFiles(params, fileUploadQuestion.get().createFileUploadQuestion())));

    DivTag skipForms = renderDeleteAndContinueFileUploadForms(params);
    DivTag buttons = renderFileUploadBottomNavButtons(params);

    return div(uploadForm, skipForms, buttons)
        .with(fileUploadViewStrategy.footerTags(params.request()));
  }

  /** Creates a tag which shows when a file is being uploaded. */
  private DivTag createFileUploadTag(ApplicationBaseViewParams params) {
    return div()
        .withText(params.messages().at(MessageKey.UPLOADING.getKeyName()))
        .withClasses(FILE_UPLOADING_TAG_CLASS, "px-2", "py-1")
        .attr("role", "alert");
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
  private DivTag renderDeleteAndContinueFileUploadForms(ApplicationBaseViewParams params) {
    String formAction =
        applicantRoutes
            .updateBlock(
                params.profile(),
                params.applicantId(),
                params.programId(),
                params.block().getId(),
                params.inReview(),
                NEXT_BLOCK)
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

  // Returns the "save and next" button for uploading multiple files. Since the upload and save
  // happens as soon as the applicant chooses the file, this instead just "submits" a no-op form
  // and moves to the next page.
  private ButtonTag renderSaveAndNextButton(ApplicationBaseViewParams params) {
    return submitButton(params.messages().at(MessageKey.BUTTON_NEXT_SCREEN.getKeyName()))
        .withForm(FILEUPLOAD_CONTINUE_FORM_ID)
        .withClasses(ButtonStyles.SOLID_BLUE, DISABLED_WHEN_UPLOADING_CLASS);
  }

  // Returns a "Previous" button that will navigate the applicant to the previous page without
  // attempting to save anything. (Since for multiple file uploads, the save is already done
  // when the user chooses a file.)
  @Override
  protected ATag renderPreviousButton(ApplicationBaseViewParams params) {
    String redirectUrl =
        params
            .applicantRoutes()
            .blockPreviousOrReview(
                params.profile(),
                params.applicantId(),
                params.programId(),
                /* currentBlockIndex= */ params.blockIndex(),
                params.inReview())
            .url();
    return a().withHref(redirectUrl)
        .withText(params.messages().at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName()))
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT, DISABLED_WHEN_UPLOADING_CLASS)
        .withId("cf-block-previous");
  }

  // Returns a "Review" button that will redirect the applicant to the review page without
  // attempting to save anything. (Since for multiple file uploads, the save is already done
  // when the user chooses a file.)
  @Override
  protected ATag renderReviewButton(ApplicationBaseViewParams params) {
    String reviewUrl =
        params
            .applicantRoutes()
            .review(params.profile(), params.applicantId(), params.programId())
            .url();
    return a().withHref(reviewUrl)
        .withText(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
        .withId("review-application-button")
        .withClasses(ButtonStyles.OUTLINED_TRANSPARENT, DISABLED_WHEN_UPLOADING_CLASS);
  }

  private DivTag renderFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, false);
  }

  private DivTag renderEmptyFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, true);
  }

  private DivTag renderFileUploadBottomNavButtons(ApplicationBaseViewParams params) {
    DivTag ret = div().withClasses(ApplicantStyles.APPLICATION_NAV_BAR);
    ret.with(renderReviewButton(params))
        .with(renderPreviousButton(params))
        .with(renderSaveAndNextButton(params));

    return ret;
  }
}
