package views.fileupload;

import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;

public final class AzureFileUploadViewStrategy extends FileUploadViewStrategy {

  @Override
  public ImmutableList<InputTag> additionalFileUploadFormInputs(
      Optional<StorageUploadRequest> request) {
    if (request.isEmpty()) {
      return ImmutableList.of();
    }
    BlobStorageUploadRequest signedRequest = castStorageRequest(request.get());
    ImmutableList.Builder<InputTag> builder = ImmutableList.builder();
    builder.add(
        input().withType("hidden").withName("fileName").withValue(signedRequest.fileName()),
        input().withType("hidden").withName("sasToken").withValue(signedRequest.sasToken()),
        input().withType("hidden").withName("blobUrl").withValue(signedRequest.blobUrl()),
        input()
            .withType("hidden")
            .withName("containerName")
            .withValue(signedRequest.containerName()),
        input().withType("hidden").withName("accountName").withValue(signedRequest.accountName()),
        input()
            .withType("hidden")
            .withName("successActionRedirect")
            .withValue(signedRequest.successActionRedirect()));
    return builder.build();
  }

  @Override
  public ImmutableMap<String, String> additionalFileUploadFormInputFields(
      Optional<StorageUploadRequest> request) {
    BlobStorageUploadRequest signedRequest = castStorageRequest(request.get());
    return ImmutableMap.of(
        "fileName", signedRequest.fileName(),
        "sasToken", signedRequest.sasToken(),
        "blobUrl", signedRequest.blobUrl(),
        "containerName", signedRequest.containerName(),
        "accountName", signedRequest.accountName(),
        "successActionRedirect", signedRequest.successActionRedirect());
  }

  private BlobStorageUploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof BlobStorageUploadRequest blobStorageUploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to Azure Blob storage using incorrect request type");
    }
    return blobStorageUploadRequest;
  }

  @Override
  public ImmutableList<ScriptTag> extraScriptTags() {
    return ImmutableList.of();
  }

  @Override
  public String getUploadFormClass() {
    return "azure-upload";
  }

  @Override
  public String getMultiFileUploadFormClass() {
    // Return a different form class for multi-file uploads so that ths TS module can vary its event
    // handling strategy.
    return "azure-multi-file-upload";
  }
}
