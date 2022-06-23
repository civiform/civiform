/**
 * This is responsible for uploading a file to Azure blob storage.
 */

class AzureUploadController {
  static FILEUPLOAD_FORM_ID = 'cf-block-form'

  constructor() {
    const blockForm = document.getElementById(
      AzureUploadController.FILEUPLOAD_FORM_ID,
    )
    blockForm.addEventListener('submit', (event) => {
      this.attemptUpload(event, blockForm)
    })
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

    const options = {
      blobHTTPHeaders: {
        blobContentType: azureUploadProps.file.type,
        blobContentDisposition: `attachment; filename=${azureUploadProps.file.name}`,
      },
    }

    const blockBlobUrl = azblob.BlockBlobURL.fromBlobURL(
      new azblob.BlobURL(
        `${azureUploadProps.blobUrl}?${azureUploadProps.sasToken}`,
        azblob.StorageURL.newPipeline(new azblob.AnonymousCredential()),
      ),
    )

    azblob
      .uploadBrowserDataToBlockBlob(
        azblob.Aborter.none,
        azureUploadProps.file,
        blockBlobUrl,
        options,
      )
      .then((resp, err) => {
        if (err) {
          throw err
        }
        this.setFileUploadMetadata(
          redirectUrl,
          azureUploadProps,
          resp,
          blockBlobUrl.url,
        )
        window.location.replace(redirectUrl.toString())
      })
  }

  private getAzureUploadProps(uploadContainer: HTMLElement) {
    return {
      sasToken: this.getValueFromInputLabel('sasToken'),
      blobUrl: this.getValueFromInputLabel('blobUrl'),
      successActionRedirect: this.getValueFromInputLabel(
        'successActionRedirect',
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
    resp: any,
    blockBlobUrlString: string,
  ) {
    redirectUrl.searchParams.set('originalFileName', azureUploadProps.file.name)
    redirectUrl.searchParams.set('etag', resp.eTag)
    redirectUrl.searchParams.set('key', azureUploadProps.fileName)
    redirectUrl.searchParams.set('bucket', azureUploadProps.containerName)
    redirectUrl.searchParams.set('blockBlobUrlString', blockBlobUrlString)
  }
}

window.addEventListener('load', () => new AzureUploadController())
