/**
 * This is responsible for uploading a file to Azure blob storage.
 */
class AzureUploadController {
  static UPLOAD_CONTAINER_ID = 'azure-upload-form-component';

  constructor() {
    const uploadContainer = document.getElementById(AzureUploadController.UPLOAD_CONTAINER_ID);
    const submitButton = uploadContainer.querySelector('button');
    submitButton.addEventListener("submit", (event) => this.attemptUpload(event, uploadContainer));
  }

  attemptUpload(event: Event, uploadContainer: HTMLElement | null) {
    if (uploadContainer != null) {
      var accountName = uploadContainer.getElementsByTagName("accountName");
      var sasToken = uploadContainer.getElementsByTagName("sasToken");
      var blobUrl = uploadContainer.getElementsByTagName("blobUrl");
      var containerName = uploadContainer.getElementsByTagName("containerName");
      var successActionRedirect = uploadContainer.getElementsByTagName("successActionRedirect");
      var fileName = uploadContainer.getElementsByTagName("fileName");
      var blobService = AzureStorage.Blob.createBlobServiceWithSas(blobUrl, sasToken);
    }
  }
}