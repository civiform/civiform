package views;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.footer;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.text;
import static j2html.attributes.Attr.FORM;
import static views.BaseHtmlView.makeCsrfTokenInputTag;
import static views.BaseHtmlView.submitButton;

import controllers.applicant.routes;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import javax.inject.Inject;
import services.MessageKey;
import services.applicant.question.FileUploadQuestion;
import services.cloud.FileNameFormatter;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;
import views.applicant.ApplicantProgramBlockEditView.Params;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;
import views.style.Styles;

public class AzureFileUploadViewStrategy extends FileUploadViewStrategy {

  private static final String AZURE_STORAGE_BLOB_WEB_JAR =
      "lib/azure__storage-blob/browser/azure-storage-blob.min.js";

  private final ViewUtils viewUtils;

  @Inject
  AzureFileUploadViewStrategy(ViewUtils viewUtils) {
    this.viewUtils = viewUtils;
  }

  @Override
  public ContainerTag signedFileUploadFields(
      ApplicantQuestionRendererParams params, FileUploadQuestion fileUploadQuestion) {
    StorageUploadRequest storageUploadRequest = params.signedFileUploadRequest().get();
    if (!(storageUploadRequest instanceof BlobStorageUploadRequest)) {
      throw new RuntimeException(
          "Trying to upload file to Azure blob storage using incorrect" + " upload request type.");
    }
    BlobStorageUploadRequest request = (BlobStorageUploadRequest) storageUploadRequest;
    ContainerTag formTag = form().withId("azure-upload-form-component");
    formTag.with(
        footer(viewUtils.makeWebJarsTag(AZURE_STORAGE_BLOB_WEB_JAR)),
        footer(viewUtils.makeLocalJsTag("azure_upload")));
    return formTag
        .with(input().withType("file").withName("file"))
        .with(input().withType("hidden").withName("sasToken").withValue(request.sasToken()))
        .with(input().withType("hidden").withName("blobUrl").withValue(request.blobUrl()))
        .with(
            input().withType("hidden").withName("containerName").withValue(request.containerName()))
        .with(input().withType("hidden").withName("fileName").withValue(request.fileName()))
        .with(input().withType("hidden").withName("accountName").withValue(request.accountName()))
        .with(
            input()
                .withType("hidden")
                .withName("successActionRedirect")
                .withValue(request.successActionRedirect()))
        .with(TagCreator.button(text("Upload to Azure Blob Storage")).withType("submit"));
  }

  @Override
  public Tag renderFileUploadBlockSubmitForms(
      Params params, ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    String key = FileNameFormatter.formatFileUploadQuestionFilename(params);
    String onSuccessRedirectUrl =
        params.baseUrl()
            + routes.ApplicantProgramBlocksController.updateFile(
                    params.applicantId(),
                    params.programId(),
                    params.block().getId(),
                    params.inReview())
                .url();

    StorageUploadRequest request =
        params.storageClient().getSignedUploadRequest(key, onSuccessRedirectUrl);

    if (!(request instanceof BlobStorageUploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to Azure Blob storage using incorrect request type");
    }
    BlobStorageUploadRequest blobStorageUploadRequest = (BlobStorageUploadRequest) request;
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setSignedFileUploadRequest(blobStorageUploadRequest)
            .build();

    ContainerTag formTag =
        form()
            .withId(BLOCK_FORM_ID)
            .with(
                each(
                    params.block().getQuestions(),
                    question ->
                        renderQuestion(
                            question, rendererParams, applicantQuestionRendererFactory)));

    Tag skipForms = renderDeleteAndContinueFileUploadForms(params);
    Tag buttons = renderFileUploadBottomNavButtons(params);

    return div(formTag, skipForms, buttons);
  }

  Tag renderFileUploadBottomNavButtons(Params params) {
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

  /**
   * Renders a form submit button for continue form if an uploaded file is present.
   *
   * <p>See {@link renderDeleteAndContinueFileUploadForms}.
   */
  Optional<Tag> maybeRenderContinueButton(Params params) {
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
    String buttonId = FILEUPLOAD_SKIP_BUTTON_ID;
    if (hasUploadedFile(params)) {
      buttonText = params.messages().at(MessageKey.BUTTON_DELETE_FILE.getKeyName());
      buttonId = FILEUPLOAD_DELETE_BUTTON_ID;
    }
    Tag button =
        submitButton(buttonText)
            .attr(FORM, FILEUPLOAD_DELETE_FORM_ID)
            .withClasses(ApplicantStyles.BUTTON_REVIEW)
            .withId(buttonId);
    return Optional.of(button);
  }

  /**
   * Returns two hidden forms for navigating through a file upload block without uploading a file.
   *
   * <p>Delete form calls a script that deletes the uploaded block blob using the JavaScript SDK. In
   * either case, the file upload question is marked as seen but unanswered, namely skipping the
   * file upload. This is only allowed for an optional question.
   *
   * <p>Continue form sends an update with the currently stored file key, the same behavior as an
   * applicant re-submits a form without changing their answer. Continue form is only used when an
   * existing file (and file key) is present.
   */
  Tag renderDeleteAndContinueFileUploadForms(Params params) {
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
            .with(
                footer(viewUtils.makeWebJarsTag(AZURE_STORAGE_BLOB_WEB_JAR)),
                footer(viewUtils.makeLocalJsTag("azure_upload")))
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderFileKeyField(question, rendererParams)));

    Tag deleteForm =
        form()
            .withId(FILEUPLOAD_DELETE_FORM_ID)
            .withAction(formAction)
            .with(footer(viewUtils.makeWebJarsTag(AZURE_STORAGE_BLOB_WEB_JAR)))
            .with(footer(viewUtils.makeLocalJsTag("azure_delete")))
            .with(makeCsrfTokenInputTag(params.request()))
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderEmptyFileKeyField(question, rendererParams)));
    return div(continueForm, deleteForm).withClasses(Styles.HIDDEN);
  }
}
