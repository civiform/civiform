/**
 * This is responsible for uploading a file to Azure blob storage.
 */

class AzureUploadController {
  static UPLOAD_CONTAINER_ID = 'azure-upload-form-component'

  constructor() {
    const uploadContainer = document.getElementById(
      AzureUploadController.UPLOAD_CONTAINER_ID
    )
    uploadContainer.addEventListener('submit', (event) =>
      this.attemptUpload(event, uploadContainer)
    )
  }

  getValueFromInputLabel(label: string): string {
    return (<HTMLInputElement>document.getElementsByName(label)[0]).value
  }

  attemptUpload(event: Event, uploadContainer: HTMLElement | null) {
    event.preventDefault()
    // This is set by an imported JavaScript Azure blob client.
    // window["azblob"] was used because the TS compiler complained about using
    // window.azblob.
    const azblob = window['azblob']
    if (uploadContainer == null) {
      throw new Error('Attempted to upload to null container')
    }
    const azureUploadProps = this.getAzureUploadProps(uploadContainer)

    const redirectUrl = new URL(azureUploadProps.successActionRedirect)

    const blockBlobUrl = azblob.BlockBlobURL.fromBlobURL(
      new azblob.BlobURL(
        `${azureUploadProps.blobUrl}?${azureUploadProps.sasToken}`,
        azblob.StorageURL.newPipeline(new azblob.AnonymousCredential())
      )
    )

    azblob
      .uploadBrowserDataToBlockBlob(
        azblob.Aborter.none,
        azureUploadProps.file,
        blockBlobUrl
      )
      .then((resp, err) => {
        if (err) {
          throw err
        }
        console.log(resp)
        this.setFileUploadMetadata(redirectUrl, azureUploadProps, resp)
        window.location.replace(redirectUrl.toString())
      })
  }

  private getAzureUploadProps(uploadContainer: HTMLElement) {
    return {
      sasToken: this.getValueFromInputLabel('sasToken'),
      blobUrl: this.getValueFromInputLabel('blobUrl'),
      successActionRedirect: this.getValueFromInputLabel(
        'successActionRedirect'
      ),
      containerName: this.getValueFromInputLabel('containerName'),
      file: (<HTMLInputElement>(
        uploadContainer.querySelector('input[type=file]')
      )).files[0],
      fileName: this.getValueFromInputLabel('fileName'),
    }
  }

  private setFileUploadMetadata(
    redirectUrl: URL,
    azureUploadProps: any,
    resp: any
  ) {
    redirectUrl.searchParams.set('userFileName', azureUploadProps.file.name)
    redirectUrl.searchParams.set('etag', resp.eTag)
    redirectUrl.searchParams.set('fileName', azureUploadProps.fileName)
    redirectUrl.searchParams.set('container', azureUploadProps.containerName)
  }
}

window.addEventListener('load', () => new AzureUploadController())
