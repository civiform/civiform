/**
 * This is responsible for uploading a file to Azure blob storage.
 */
class AzureUploadController {
  static UPLOAD_CONTAINER_ID = 'azure-upload-form-component';

  constructor() {
    const uploadContainer = document.getElementById(AzureUploadController.UPLOAD_CONTAINER_ID);
    uploadContainer.addEventListener("submit", (event) => this.attemptUpload(event, uploadContainer));
  }

  getValueFromInputLabel(label: string): string {
    return (<HTMLInputElement>document.getElementsByName(label)[0]).value;
  }

  attemptUpload(event: Event, uploadContainer: HTMLElement | null) {
    event.preventDefault();
    const azblob = window["azblob"];
    if (uploadContainer == null) {
      console.error("Attempted to upload to null container");
    }
    const sasToken = this.getValueFromInputLabel("sasToken");
    let blobUrl = this.getValueFromInputLabel("blobUrl");
    if (blobUrl.includes("azurite")) { // TODO: replace this with a regex to detect azurite URLs
      blobUrl = blobUrl.replace("azurite", "localhost");
    }
    const successActionRedirect = this.getValueFromInputLabel("successActionRedirect");
    const containerName = this.getValueFromInputLabel("containerName");
    const file = (<HTMLInputElement>uploadContainer.querySelector('input[type=file]')).files[0];
    let fileName = this.getValueFromInputLabel("fileName");

    const redirectUrl = new URL(successActionRedirect);

    const blockBlobURL = azblob.BlockBlobURL.fromBlobURL(
        new azblob.BlobURL(
            `${blobUrl}?${sasToken}`,
            azblob.StorageURL.newPipeline(new azblob.AnonymousCredential)
        ));
    azblob.uploadBrowserDataToBlockBlob(azblob.Aborter.none, file, blockBlobURL).then((resp, err) => {
      if (err) {
        console.log(err);
      } else {
        console.log(resp);
        redirectUrl.searchParams.set("userFileName", file.name)
        redirectUrl.searchParams.set("etag", resp.eTag);
        redirectUrl.searchParams.set("fileName", fileName);
        redirectUrl.searchParams.set("container", containerName);
        window.location.replace(redirectUrl.toString());
      }
    });
  }
}

let azureUploadController = new AzureUploadController();
