package views.dev;

import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.text;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import javax.inject.Inject;
import play.Environment;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;
import views.HtmlBundle;
import views.ViewUtils;

/** Strategy class for creating a file upload form for Azure. */
public class AzureViewStorageStrategy implements FileUploadViewStorageStrategy {

  private static final String AZURE_STORAGE_BLOB_CDN =
      "https://cdn.jsdelivr.net/npm/@azure/storage-blob@10.5.0/browser/azure-storage-blob.min.js";

  private final Environment environment;

  @Inject
  private AzureViewStorageStrategy(Environment environment) {
    this.environment = environment;
  }

  @Override
  public ContainerTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle) {
    if (!(storageUploadRequest instanceof BlobStorageUploadRequest)) {
      return null;
    }
    BlobStorageUploadRequest request = (BlobStorageUploadRequest) storageUploadRequest;

    // Make sure the CDN is only being injected in a dev environment.
    // This should always be true, since this is the dev file upload view.
    if (environment.isDev()) {
      bundle.addFooterScripts(viewUtils.makeCdnJsTag(AZURE_STORAGE_BLOB_CDN));
    }
    bundle.addFooterScripts(viewUtils.makeLocalJsTag("azure_upload"));

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
