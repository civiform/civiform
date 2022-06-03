package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import j2html.tags.Tag;
import javax.inject.Inject;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;

public final class AzureFileUploadViewStrategy extends FileUploadViewStrategy {

  private final ViewUtils viewUtils;

  @Inject
  AzureFileUploadViewStrategy(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  @Override
  protected ImmutableList<Tag> extraFileUploadFields(StorageUploadRequest request) {
    BlobStorageUploadRequest signedRequest = castStorageRequest(request);
    ImmutableList.Builder<Tag> builder = ImmutableList.builder();
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
