package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import javax.inject.Inject;
import services.applicant.question.FileUploadQuestion;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;
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

  private BlobStorageUploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof BlobStorageUploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to Azure Blob storage using incorrect request type");
    }
    return (BlobStorageUploadRequest) request;
  }

  @Override
  protected ImmutableList<Tag> extraScriptTags() {
    return ImmutableList.of(
        viewUtils.makeAzureBlobStoreScriptTag(),
        viewUtils.makeLocalJsTag("azure_upload"),
        viewUtils.makeLocalJsTag("azure_delete"));
  }
}
