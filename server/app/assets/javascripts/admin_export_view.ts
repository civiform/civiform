import {assertNotNull} from './util'

class AdminExportView {
  // These values should be kept in sync with views/admin/migration/AdminExportView.java.
  private static PROGRAM_JSON_ID = 'program-json'
  private static COPY_BUTTON_ID = 'copy-json-button'

  constructor() {
    this.addClickListenerToCopyButton()
  }

  addClickListenerToCopyButton() {
    const copyButton = document.getElementById(AdminExportView.COPY_BUTTON_ID)
    if (copyButton) {
      copyButton.addEventListener('click', () => {
        const json = assertNotNull(
          document.getElementById(AdminExportView.PROGRAM_JSON_ID),
        )
        void writeClipboardText(json.innerHTML)
        alert('Copied the JSON to the clipboard')
      })
    }

    async function writeClipboardText(text: string) {
      await navigator.clipboard.writeText(text)
    }
  }
}

export function init() {
  new AdminExportView()
}
