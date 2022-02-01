package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.text;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import javax.inject.Inject;
import models.StoredFile;
import services.cloud.StorageClient;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;
import views.HtmlBundle;
import views.ViewUtils;

/** Strategy class for creating a file upload form for Azure. */
public class AzureStorageDevViewStrategy implements CloudStorageDevViewStrategy {

  private static final String AZURE_STORAGE_BLOB_WEB_JAR =
      "lib/azure__storage-blob/browser/azure-storage-blob.min.js";

  private final StorageClient client;

  @Inject
  public AzureStorageDevViewStrategy(StorageClient client) {
    this.client = checkNotNull(client);
  }

  @Override
  public ContainerTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle)
      throws RuntimeException {
    if (!(storageUploadRequest instanceof BlobStorageUploadRequest)) {
      throw new RuntimeException(
          "Trying to upload file to dev Azurite (Azure emulator) blob storage using incorrect"
              + " upload request type.");
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

  @Override
  public ContainerTag renderFiles(ImmutableList<StoredFile> files) {
    return table()
        .with(
            tbody(
                each(
                    files,
                    file ->
                        tr(
                            td(String.valueOf(file.id)),
                            td(
                                a(file.getOriginalFileName().isPresent()
                                        ? file.getOriginalFileName().get()
                                        : file.getName())
                                    .withHref(getPresignedUrl(file)))))));
  }

  @Override
  public String getPresignedUrl(StoredFile file) {
    return client.getPresignedUrlString(file.getName(), file.getOriginalFileName());
  }
}
