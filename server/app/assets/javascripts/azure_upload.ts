/**
 * This is responsible for uploading a file to Azure blob storage.
 */

// TODO(#3994): update Azure storage blob dependency and remove exceptions
/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-unsafe-assignment */
/* eslint-disable @typescript-eslint/no-unsafe-call */
/* eslint-disable @typescript-eslint/no-unsafe-argument */
/* eslint-disable @typescript-eslint/no-explicit-any */

import {assertNotNull} from './util'

class AzureUploadController {
  private static FILEUPLOAD_FORM_ID = 'cf-block-form'
  private static AZURE_UPLOAD_SELECTOR = '.azure-upload'

  constructor() {
    if (
      document.querySelector(AzureUploadController.AZURE_UPLOAD_SELECTOR) ==
      null
    ) {
      return
    }
    const blockForm = assertNotNull(
      document.getElementById(AzureUploadController.FILEUPLOAD_FORM_ID),
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
    const azblob = (window as {[key: string]: any})['azblob']

    if (uploadContainer == null) {
      throw new Error('Attempted to upload to null container')
    }
    const azureUploadProps = this.getAzureUploadProps(uploadContainer)
    if (azureUploadProps.file == null) {
      // No file selected by the user. Validation is done in file_upload.ts so
      // here we simply halt uploading.
      return
    }

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
      .then((resp: any, err: any) => {
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
    const files = assertNotNull(
      uploadContainer.querySelector<HTMLInputElement>('input[type=file]')
        ?.files,
    )
    return {
      sasToken: this.getValueFromInputLabel('sasToken'),
      blobUrl: this.getValueFromInputLabel('blobUrl'),
      successActionRedirect: this.getValueFromInputLabel(
        'successActionRedirect',
      ),
      containerName: this.getValueFromInputLabel('containerName'),
      file: files[0],
      fileName: this.getValueFromInputLabel('fileName'),
    }
  }
  /* eslint-disable  @typescript-eslint/no-explicit-any */
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
/* eslint-enable  @typescript-eslint/no-explicit-any */

export function init() {
  new AzureUploadController()
}
