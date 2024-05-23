import {assertNotNull} from './util'

class AdminExportView {
  private static JSON_SELECTOR = 'program-json'
  private static COPY_BUTTON_SELECTOR = 'copy-json-button'

  constructor() {
    document.addEventListener('htmx:afterSwap', function () {
      // htmx:afterSwap fires after htmx swaps in new content
      // the json and copy button are only availabe once htmx has swapped them in
      const copyButton = assertNotNull(
        document.getElementById(AdminExportView.COPY_BUTTON_SELECTOR),
      )
      copyButton.addEventListener('click', () => {
        const json = assertNotNull(
          document.getElementById(AdminExportView.JSON_SELECTOR),
        )
        void writeClipboardText(json.innerHTML)
        alert('Copied the Json to the clipboard')
      })
    })

    async function writeClipboardText(text: string) {
      await navigator.clipboard.writeText(text)
    }
  }
}

export function init() {
  new AdminExportView()
}
