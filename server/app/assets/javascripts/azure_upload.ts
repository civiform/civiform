/**
 * This is responsible for uploading a file to Azure blob storage.
 */

// TODO(#3994): update Azure storage blob dependency and remove exceptions
/* eslint-disable @typescript-eslint/no-unsafe-member-access */

/* eslint-disable @typescript-eslint/no-unsafe-argument */
/* eslint-disable @typescript-eslint/no-explicit-any */

import {assertNotNull} from './util'
import {BlockBlobClient} from '@azure/storage-blob'

class AzureUploadController {
  private static AZURE_UPLOAD_SELECTOR = '.azure-upload'
  private static AZURE_MULTI_FILE_UPLOAD_SELECTOR = '.azure-multi-file-upload'

  constructor(formId: string) {
    const uploadForm = document.querySelector(
      AzureUploadController.AZURE_UPLOAD_SELECTOR,
    )
    const multiFileUploadForm = document.querySelector(
      AzureUploadController.AZURE_MULTI_FILE_UPLOAD_SELECTOR,
    )

    if (uploadForm == null && multiFileUploadForm == null) {
      // No Azure file upload forms were found.
      return
    }

    const blockForm = assertNotNull(document.getElementById(formId))
    if (uploadForm != null) {
      // On single file uploads, we upload the file when the user hits the submit button.
      blockForm.addEventListener('submit', (event) => {
        this.attemptUpload(event, blockForm)
      })
    }

    if (multiFileUploadForm != null) {
      // On multi-file uploads, we upload the file when the user chooses a file.
      blockForm.addEventListener('change', (event) => {
        this.attemptUpload(event, blockForm)
      })
    }
  }

  getValueFromInputLabel(label: string): string {
    return (<HTMLInputElement>document.getElementsByName(label)[0]).value
  }

  attemptUpload(event: Event, uploadContainer: HTMLElement | null) {
    event.preventDefault()

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

    const fullBlobUrlWithSas = `${azureUploadProps.blobUrl}?${azureUploadProps.sasToken}`

    const blockBlobClient = new BlockBlobClient(fullBlobUrlWithSas)

    const options = {
      blobHTTPHeaders: {
        blobContentType: azureUploadProps.file.type,
        blobContentDisposition: `attachment; filename=${azureUploadProps.file.name}`,
      },
    }

    blockBlobClient
      .uploadData(azureUploadProps.file, options)
      .then((resp: any) => {
        this.setFileUploadMetadata(
          redirectUrl,
          azureUploadProps,
          resp,
          blockBlobClient.url,
        )
        window.location.replace(redirectUrl.toString())
      })
      .catch((err: any) => {
        throw err
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

export function init(formId: string) {
  new AzureUploadController(formId)
}
