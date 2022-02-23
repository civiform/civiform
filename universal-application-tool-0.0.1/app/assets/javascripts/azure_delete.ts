/**
 * This is responsible for deleting a file that was uploaded to Azure blob storage.
 */
class AzureDeleteController {
  private static FILEUPLOAD_DELETE_ID = 'fileupload-delete-button'

  constructor() {
    const deleteContainer = document.getElementById(
      AzureDeleteController.FILEUPLOAD_DELETE_ID
    )
    const azblob = window['azblob']
    deleteContainer.addEventListener('click', (event) =>
      this.attemptDelete(azblob)
    )
  }

  attemptDelete(azblob: any) {
    const blockBlobUrl = this.getBlockBlobUrl(azblob)
    if (!blockBlobUrl) {
      throw new Error(
        'Attempting to delete file from a block blob URL that does not exist'
      )
    }
    blockBlobUrl.delete(azblob.Aborter.none)
  }

  private getBlockBlobUrl(azblob: any) {
    const searchParams = new URLSearchParams(document.location.search)
    const blockBlobUrlString = searchParams.get('blockBlobUrlString')
    if (!blockBlobUrlString) {
      throw new Error('Attempting to delete file that does not exist')
    }
    return new azblob.BlockBlobURL(blockBlobUrlString)
  }
}

window.addEventListener('load', () => new AzureDeleteController())
