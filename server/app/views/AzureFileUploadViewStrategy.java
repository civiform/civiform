package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.footer;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.attributes.Attr.ENCTYPE;
import static j2html.attributes.Attr.FORM;

import controllers.applicant.routes;

import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.FooterTag;

import java.util.Optional;
import javax.inject.Inject;
import services.MessageKey;
import services.applicant.question.FileUploadQuestion;
import services.cloud.FileNameFormatter;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;
import views.style.Styles;

public class AzureFileUploadViewStrategy extends FileUploadViewStrategy {

  private final ViewUtils viewUtils;

  @Inject
  AzureFileUploadViewStrategy(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  @Override
  public FormTag signedFileUploadFields(
      ApplicantQuestionRendererParams params, FileUploadQuestion fileUploadQuestion) {
    StorageUploadRequest storageUploadRequest = params.signedFileUploadRequest().get();

    BlobStorageUploadRequest request = castStorageRequest(storageUploadRequest);

    Optional<String> uploaded =
        fileUploadQuestion.getFilename().map(f -> String.format("File uploaded: %s", f));

    FormTag formTag = form();
    return formTag
        .with(div().withText(uploaded.orElse("")))
        .with(input().attr("type", "file").attr("name", "file"))
        .with(input().attr("type", "hidden").attr("name", "fileName").attr("value", request.fileName()))
        .with(input().attr("type", "hidden").attr("name", "sasToken").attr("value", request.sasToken()))
        .with(input().attr("type", "hidden").attr("name", "blobUrl").attr("value", request.blobUrl()))
        .with(
            input().attr("type", "hidden").attr("name", "containerName").attr("value", request.containerName()))
        .with(input().attr("type", "hidden").attr("name", "accountName").attr("value", request.accountName()))
        .with(
            input()
                .attr("type", "hidden")
                .attr("name", "successActionRedirect")
                .attr("value", request.successActionRedirect()))
        .with(errorDiv(params.messages(), fileUploadQuestion));
  }

  @Override
  public DivTag renderFileUploadBlockSubmitForms(
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

    BlobStorageUploadRequest blobStorageUploadRequest = castStorageRequest(request);

    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setSignedFileUploadRequest(blobStorageUploadRequest)
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    FormTag formTag =
        form()
            .withId(BLOCK_FORM_ID)
            .attr(ENCTYPE, "multipart/form-data")
            .with(
                each(
                    params.block().getQuestions(),
                    question ->
                        renderQuestion(
                            question, rendererParams, applicantQuestionRendererFactory)));

    DivTag skipForms = renderDeleteAndContinueFileUploadForms(params);
    DivTag buttons = renderFileUploadBottomNavButtons(params);

    return div(formTag, skipForms, buttons)
        .withId("azure-upload-form-component")
        .with(
            footer(viewUtils.makeAzureBlobStoreScriptTag()),
            footer(viewUtils.makeLocalJsTag("azure_upload")));
  }

  @Override
  protected Optional<ButtonTag> maybeRenderSkipOrDeleteButton(Params params) {
    if (hasAtLeastOneRequiredQuestion(params)) {
      // If the file question is required, skip or delete is not allowed.
      return Optional.empty();
    }
    String buttonText = params.messages().at(MessageKey.BUTTON_SKIP_FILEUPLOAD.getKeyName());
    String buttonId = FILEUPLOAD_SKIP_BUTTON_ID;
    Optional<FooterTag> footer = Optional.empty();

    if (hasUploadedFile(params)) {
      buttonText = params.messages().at(MessageKey.BUTTON_DELETE_FILE.getKeyName());
      buttonId = FILEUPLOAD_DELETE_BUTTON_ID;
      footer = Optional.of(footer().with(viewUtils.makeLocalJsTag("azure_delete")));
    }

    ButtonTag button =
        button(buttonText)
            .attr(FORM, FILEUPLOAD_DELETE_FORM_ID)
            .withClasses(ApplicantStyles.BUTTON_REVIEW)
            .withId(buttonId);
    footer.ifPresent(button::with);

    return Optional.of(button);
  }

  private BlobStorageUploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof BlobStorageUploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to Azure Blob storage using incorrect request type");
    }
    return (BlobStorageUploadRequest) request;
  }

  private DivTag renderFileUploadBottomNavButtons(Params params) {
    Optional<ButtonTag> maybeContinueButton = maybeRenderContinueButton(params);
    Optional<ButtonTag> maybeSkipOrDeleteButton = maybeRenderSkipOrDeleteButton(params);
    DivTag ret =
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
