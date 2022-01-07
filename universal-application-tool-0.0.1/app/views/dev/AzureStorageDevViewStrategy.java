package views.dev;

import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.text;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;
import views.HtmlBundle;
import views.ViewUtils;

/** Strategy class for creating a file upload form for Azure. */
public class AzureStorageDevViewStrategy implements CloudStorageDevViewStrategy {

  private static final String AZURE_STORAGE_BLOB_WEB_JAR =
      "lib/azure__storage-blob/browser/azure-storage-blob.min.js";

  @Override
  public ContainerTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle) {
    if (!(storageUploadRequest instanceof BlobStorageUploadRequest)) {
      return null;
    }
    BlobStorageUploadRequest request = (BlobStorageUploadRequest) storageUploadRequest;
    bundle.addFooterScripts(
        viewUtils.makeWebJarsTag(/* assetsRoute= */ AZURE_STORAGE_BLOB_WEB_JAR));
    bundle.addFooterScripts(viewUtils.makeLocalJsTag(/* filename= */ "azure_upload"));

    ContainerTag formTag = form().withId("azure-upload-form-component");

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
}
