// TODO(#3994): update Azure storage blob dependency and remove exceptions
/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-unsafe-assignment */
/* eslint-disable @typescript-eslint/no-unsafe-call */
/* eslint-disable @typescript-eslint/no-unsafe-return */
/* eslint-disable @typescript-eslint/no-unsafe-argument */
/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * This is responsible for deleting a file that was uploaded to Azure blob storage.
 */
class AzureDeleteController {
  private static FILEUPLOAD_DELETE_ID = 'fileupload-delete-button'
  private static AZURE_UPLOAD_SELECTOR = '.azure-upload'

  constructor() {
    if (
      document.querySelector(AzureDeleteController.AZURE_UPLOAD_SELECTOR) ==
      null
    ) {
      return
    }
    const deleteContainer = document.getElementById(
      AzureDeleteController.FILEUPLOAD_DELETE_ID,
    )
    if (deleteContainer) {
      const azblob = (window as {[key: string]: any})['azblob']
      deleteContainer.addEventListener('click', () =>
        this.attemptDelete(azblob),
      )
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  attemptDelete(azblob: any) {
    const blockBlobUrl = this.getBlockBlobUrl(azblob)
    if (!blockBlobUrl) {
      throw new Error(
        'Attempting to delete file from a block blob URL that does not exist',
      )
    }
    blockBlobUrl.delete(azblob.Aborter.none)
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private getBlockBlobUrl(azblob: any) {
    const searchParams = new URLSearchParams(document.location.search)
    const blockBlobUrlString = searchParams.get('blockBlobUrlString')
    if (!blockBlobUrlString) {
      throw new Error('Attempting to delete file that does not exist')
    }
    return new azblob.BlockBlobURL(blockBlobUrlString)
  }
}

export function init() {
  new AzureDeleteController()
}
