package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.footer;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.attributes.Attr.ENCTYPE;

import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import javax.inject.Inject;
import services.applicant.question.FileUploadQuestion;
import services.cloud.FileNameFormatter;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;

public class AzureFileUploadViewStrategy extends FileUploadViewStrategy {

  private final ViewUtils viewUtils;

  @Inject
  AzureFileUploadViewStrategy(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  @Override
  public ContainerTag signedFileUploadFields(
      ApplicantQuestionRendererParams params, FileUploadQuestion fileUploadQuestion) {
    StorageUploadRequest storageUploadRequest = params.signedFileUploadRequest().get();

    BlobStorageUploadRequest request = castStorageRequest(storageUploadRequest);

    Optional<String> uploaded =
        fileUploadQuestion.getFilename().map(f -> String.format("File uploaded: %s", f));

    ContainerTag formTag = form();
    return formTag
        .with(div().withText(uploaded.orElse("")))
        .with(input().withType("file").withName("file"))
        .with(input().withType("hidden").withName("fileName").withValue(request.fileName()))
        .with(input().withType("hidden").withName("sasToken").withValue(request.sasToken()))
        .with(input().withType("hidden").withName("blobUrl").withValue(request.blobUrl()))
        .with(
            input().withType("hidden").withName("containerName").withValue(request.containerName()))
        .with(input().withType("hidden").withName("accountName").withValue(request.accountName()))
        .with(
            input()
                .withType("hidden")
                .withName("successActionRedirect")
                .withValue(request.successActionRedirect()))
        .with(errorDiv(params.messages(), fileUploadQuestion));
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

    BlobStorageUploadRequest blobStorageUploadRequest = castStorageRequest(request);

    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setSignedFileUploadRequest(blobStorageUploadRequest)
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    ContainerTag formTag =
        form()
            .withId(BLOCK_FORM_ID)
            .attr(ENCTYPE, "multipart/form-data")
            .with(
                each(
                    params.block().getQuestions(),
                    question ->
                        renderQuestion(
                            question, rendererParams, applicantQuestionRendererFactory)));

    Tag skipForms = renderDeleteAndContinueFileUploadForms(params);
    Tag buttons = renderFileUploadBottomNavButtons(params);

    return div(formTag, skipForms, buttons)
        .withId("azure-upload-form-component")
        .with(
            footer(viewUtils.makeAzureBlobStoreScriptTag()),
            footer(viewUtils.makeLocalJsTag("azure_upload")),
            footer(viewUtils.makeLocalJsTag("azure_delete")));
  }

  private BlobStorageUploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof BlobStorageUploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to Azure Blob storage using incorrect request type");
    }
    return (BlobStorageUploadRequest) request;
  }
}
