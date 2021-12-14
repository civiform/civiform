/**
 * This is responsible for uploading a file to Azure blob storage.
 */
class AzureUploadController {
  static UPLOAD_CONTAINER_ID = 'azure-upload-form-component';
  static UPLOAD_PROGRESS_BAR_ID = 'azure-upload-progress-component';

  constructor() {
    const uploadContainer = document.getElementById(AzureUploadController.UPLOAD_CONTAINER_ID);
    uploadContainer.addEventListener("submit", (event) => this.attemptUpload(event, uploadContainer));
  }

  getValueFromInputLabel(label:string) {
    return (<HTMLInputElement>document.getElementsByName(label)[0]).value;
  }

  attemptUpload(event: Event, uploadContainer: HTMLElement | null) {
    event.preventDefault();
    const azblob = window["azblob"]; 
    if (uploadContainer != null) {
      const sasToken = this.getValueFromInputLabel("sasToken");
      const blobUrl = this.getValueFromInputLabel("blobUrl");
      const successActionRedirect = this.getValueFromInputLabel("successActionRedirect");
      const containerName = this.getValueFromInputLabel("containerName");
      const accountName = this.getValueFromInputLabel("accountName");
      const fileName = this.getValueFromInputLabel("fileName");

      const file = (<HTMLInputElement>uploadContainer.querySelector('input[type=file]')).files[0];
      const redirectUrl = new URL(successActionRedirect);


      const blockBlobURL = azblob.BlockBlobURL.fromBlobURL(
        new azblob.BlobURL(
          `${blobUrl}?${sasToken}`,
          azblob.StorageURL.newPipeline(new azblob.AnonymousCredential)
        ));
      azblob.uploadBrowserDataToBlockBlob(azblob.Aborter.none, file, blockBlobURL).then((result, err) => {
        if (err) {
          console.log(err);
        } else {
          console.log(result);  
        }      
      });
      
     }
  }
}
let azureUploadController = new AzureUploadController();
