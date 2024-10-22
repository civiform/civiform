import {assertNotNull} from './util'

class AdminApplicationView {
  private static APPLICATION_STATUS_SELECTOR =
    '.cf-program-admin-status-selector'
  private static CONFIRM_MODAL_FOR_DATA_ATTRIBUTE =
    'data-status-update-confirm-for-status'
  private static APPLICATION_STATUS_UPDATE_FORM_SELECTOR =
    '.cf-program-admin-status-update-form'
  private static APPLICATION_EDIT_NOTE_FORM_SELECTOR =
    '.cf-program-admin-edit-note-form'

  // These values should be kept in sync with those in admin_applications.ts and
  // ProgramApplicationView.java.
  private static PROGRAM_ID_INPUT_NAME = 'programId'
  private static APPLICATION_ID_INPUT_NAME = 'applicationId'
  private static CURRENT_STATUS_INPUT_NAME = 'currentStatus'
  private static NEW_STATUS_INPUT_NAME = 'newStatus'
  private static SEND_EMAIL_INPUT_NAME = 'sendEmail'
  private static NOTE_INPUT_NAME = 'note'

  constructor() {
    this.registerStatusSelectorEventListener()
    this.registerStatusUpdateFormSubmitListeners()
    this.registerEditNoteFormSubmitListener()
    this.registerBulkSelectCheckboxListener()
  }
  private registerBulkSelectCheckboxListener() {
    // Get the "select all" checkbox
    const selectAllCheckbox = <HTMLInputElement>(
      document.getElementById('selectAll')
    ) // Replace "selectAll" with the ID of your checkbox

    // Get all the other checkboxes
    const checkboxes = <HTMLInputElement>(
      document.querySelectorAll('input[type="checkbox"]:not(#selectAll)')
    ) // Exclude the "select all" checkbox
    selectAllCheckbox.addEventListener('click', function () {
      for (const checkbox of checkboxes) {
        // Avoid toggling the "select all" checkbox itself
        checkbox.checked = selectAllCheckbox.checked
      }
    })
  }

  private registerStatusUpdateFormSubmitListeners() {
    const statusUpdateForms = Array.from(
      document.querySelectorAll(
        AdminApplicationView.APPLICATION_STATUS_UPDATE_FORM_SELECTOR,
      ),
    )
    for (const statusUpdateForm of statusUpdateForms) {
      // The application list needs to reflect the updated status, so we use postMessage to send a
      // request to the main frame to update the status rather than submitting the form directly
      // within the IFrame.
      statusUpdateForm.addEventListener('submit', (ev) => {
        ev.preventDefault()
        const formEl = assertNotNull(ev.target as HTMLFormElement)
        window.parent.postMessage(
          {
            messageType: 'UPDATE_STATUS',
            programId: parseInt(
              this.extractInputValueFromForm(
                formEl,
                AdminApplicationView.PROGRAM_ID_INPUT_NAME,
              ),
              10,
            ),
            applicationId: parseInt(
              this.extractInputValueFromForm(
                formEl,
                AdminApplicationView.APPLICATION_ID_INPUT_NAME,
              ),
              10,
            ),
            data: {
              currentStatus: this.extractInputValueFromForm(
                formEl,
                AdminApplicationView.CURRENT_STATUS_INPUT_NAME,
              ),
              newStatus: this.extractInputValueFromForm(
                formEl,
                AdminApplicationView.NEW_STATUS_INPUT_NAME,
              ),
              sendEmail: this.extractCheckboxInputValueFromForm(
                formEl,
                AdminApplicationView.SEND_EMAIL_INPUT_NAME,
              ),
            },
          },
          window.location.origin,
        )
      })
    }
  }

  private registerEditNoteFormSubmitListener() {
    const editNoteForm = document.querySelector(
      AdminApplicationView.APPLICATION_EDIT_NOTE_FORM_SELECTOR,
    )
    if (!editNoteForm) {
      return
    }
    // Use postMessage to send a request to the main frame to update the note rather than
    // submitting the form directly within the IFrame. This allows the main frame to update the
    // list of applications to reflect the note change.
    editNoteForm.addEventListener('submit', (ev) => {
      ev.preventDefault()
      const formEl = assertNotNull(ev.target as HTMLFormElement)
      window.parent.postMessage(
        {
          messageType: 'EDIT_NOTE',
          programId: parseInt(
            this.extractInputValueFromForm(
              formEl,
              AdminApplicationView.PROGRAM_ID_INPUT_NAME,
            ),
            10,
          ),
          applicationId: parseInt(
            this.extractInputValueFromForm(
              formEl,
              AdminApplicationView.APPLICATION_ID_INPUT_NAME,
            ),
            10,
          ),
          data: {
            note: this.extractInputValueFromForm(
              formEl,
              AdminApplicationView.NOTE_INPUT_NAME,
            ),
          },
        },
        window.location.origin,
      )
    })
  }

  private extractInputValueFromForm(
    formEl: HTMLFormElement,
    inputName: string,
  ): string {
    return assertNotNull(
      formEl.querySelector<HTMLInputElement>(`[name=${inputName}]`),
      inputName,
    ).value
  }

  private extractCheckboxInputValueFromForm(
    formEl: HTMLFormElement,
    inputName: string,
  ): string {
    const checkbox = assertNotNull(
      formEl.querySelector<HTMLInputElement>(`[name=${inputName}]`),
      inputName,
    )
    return checkbox.checked ? checkbox.value : ''
  }

  private registerStatusSelectorEventListener() {
    const statusSelectForm = document.querySelector<HTMLFormElement>(
      AdminApplicationView.APPLICATION_STATUS_SELECTOR,
    )
    if (!statusSelectForm) {
      // If status tracking isn't enabled, there's nothing to do.
      return
    }
    const statusSelector = assertNotNull(
      statusSelectForm.querySelector<HTMLSelectElement>('select'),
    )

    // Remember the original value here since neither the 'change/input' events provide the
    // previous selected value. We need to reset the value when the confirmation is rejected.
    const originalSelectedValue = statusSelector.value
    statusSelector.addEventListener('change', () => {
      // Upon confirmation the model is responsible for reloading the page with the new status, so
      // don't await it and pro-actively reset the selection to the previous value for when the
      // user cancels the confirmation.
      this.showConfirmStatusChangeModal(statusSelector.value)
      statusSelector.value = originalSelectedValue
    })
  }

  private showConfirmStatusChangeModal(selectedStatus: string) {
    // Find the modal with the data attribute associating it with the selected status.
    const statusModalTriggers = Array.from(
      document.querySelectorAll(
        `[${AdminApplicationView.CONFIRM_MODAL_FOR_DATA_ATTRIBUTE}]`,
      ),
    )
    const relevantStatusModalTrigger = assertNotNull(
      statusModalTriggers.find((statusModalTrigger) => {
        return (
          selectedStatus ===
          statusModalTrigger.getAttribute(
            AdminApplicationView.CONFIRM_MODAL_FOR_DATA_ATTRIBUTE,
          )
        )
      }) as HTMLButtonElement | null,
    )
    relevantStatusModalTrigger.click()
  }
}

export function init() {
  new AdminApplicationView()
}
