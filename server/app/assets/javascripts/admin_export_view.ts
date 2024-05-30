import {assertNotNull} from './util'

class AdminExportView {
  // These values should be kept in sync with views/admin/migration/AdminExportView.java.
  private static PROGRAM_EXPORT_FORM_ID = 'program-export-form'
  private static GENERATE_JSON_BUTTON_ID = 'generate-json-button'
  // These values should be kept in sync with views/admin/migration/AdminExportViewPartial.java.
  private static PROGRAM_JSON_ID = 'program-json'
  private static COPY_BUTTON_ID = 'copy-json-button'

  constructor() {
    this.addSelectListenerToForm()
    this.addClickListenerToCopyButton()
  }

  addSelectListenerToForm() {
    const programExportForm = document.getElementById(
      AdminExportView.PROGRAM_EXPORT_FORM_ID,
    )
    if (programExportForm) {
      const generateJsonButton = assertNotNull(
        document.getElementById(AdminExportView.GENERATE_JSON_BUTTON_ID),
      ) as HTMLButtonElement
      programExportForm.addEventListener('change', () => {
        const options = document.querySelectorAll('input[name="programId"]')
        // The "Generate JSON" button is disabled by default
        // If any of the options are selected we want to enable the button
        options.forEach((option) => {
          const radioButton = option as HTMLInputElement
          if (radioButton.checked) {
            generateJsonButton.disabled = false
          }
        })
      })
    }
  }

  addClickListenerToCopyButton() {
    document.addEventListener('htmx:afterSwap', () => {
      // htmx:afterSwap fires after htmx swaps in new content
      // the json and copy button are only availabe once htmx has swapped them in
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
    })

    async function writeClipboardText(text: string) {
      await navigator.clipboard.writeText(text)
    }
  }
}

export function init() {
  new AdminExportView()
}
