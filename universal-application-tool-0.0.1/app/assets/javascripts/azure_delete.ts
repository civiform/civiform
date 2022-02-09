/**
 * This is responsible for deleting a file that was uploaded to Azure blob storage.
 */

class AzureDeleteController {
  constructor() {
    const deleteContainer = document.getElementById('fileupload-delete-button')
    deleteContainer.addEventListener('click', (event) => this.attemptDelete())
  }

  attemptDelete() {
    const azblob = window['azblob']
    const blockBlobUrl = this.getBlockBlobUrl()
    if (blockBlobUrl == null) {
      return
    }
    blockBlobUrl.delete(azblob.Aborter.none)
    console.log(blockBlobUrl)
  }

  private getBlockBlobUrl() {
    const azblob = window['azblob']
    const searchParams = new URLSearchParams(document.location.search)
    return new azblob.BlockBlobURL(searchParams.get('blockBlobUrlString'))
  }
}

window.addEventListener('load', () => new AzureDeleteController())
