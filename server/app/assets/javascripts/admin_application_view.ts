class AdminApplicationView {
  private static APPLICATION_STATUS_SELECTOR =
    '.cf-program-admin-status-selector'
  private static CONFIRM_MODAL_FOR_DATA_ATTRIBUTE =
    'data-status-update-confirm-for-status'
  private static APPLICATION_STATUS_UPDATE_FORM_SELECTOR =
    '.cf-program-admin-status-update-form'
  private static APPLICATION_EDIT_NOTE_FORM_SELECTOR =
    '.cf-program-admin-edit-note-form'

  constructor() {
    this.registerStatusSelectorEventListener()
    this.registerStatusUpdateFormSubmitListeners()
    this.registerEditNoteFormSubmitListener()
  }

  private registerStatusUpdateFormSubmitListeners() {
    const statusUpdateForms = Array.from(
      document.querySelectorAll(
        AdminApplicationView.APPLICATION_STATUS_UPDATE_FORM_SELECTOR,
      ),
    )
    for (const statusUpdateForm of statusUpdateForms) {
      statusUpdateForm.addEventListener('submit', (ev) => {
        ev.preventDefault()
        const formEl = this._assertNotNull(
          ev.target as HTMLFormElement,
          'form element',
        )
        window.parent.postMessage(
          {
            messageType: 'UPDATE_STATUS',
            programId: parseInt(
              this.extractInputValueFromForm(formEl, 'programId'),
              10,
            ),
            applicationId: parseInt(
              this.extractInputValueFromForm(formEl, 'applicationId'),
              10,
            ),
            data: {
              newStatus: this.extractInputValueFromForm(formEl, 'newStatus'),
              sendEmail: this.extractInputValueFromForm(formEl, 'sendEmail'),
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
    editNoteForm.addEventListener('submit', (ev) => {
      ev.preventDefault()
      const formEl = this._assertNotNull(
        ev.target as HTMLFormElement,
        'form element',
      )
      window.parent.postMessage(
        {
          messageType: 'EDIT_NOTE',
          programId: parseInt(
            this.extractInputValueFromForm(formEl, 'programId'),
            10,
          ),
          applicationId: parseInt(
            this.extractInputValueFromForm(formEl, 'applicationId'),
            10,
          ),
          data: {
            note: this.extractInputValueFromForm(formEl, 'note'),
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
    return this._assertNotNull(
      formEl.querySelector(`[name=${inputName}]`) as HTMLInputElement,
      inputName,
    ).value
  }

  private registerStatusSelectorEventListener() {
    const statusSelectForm = document.querySelector(
      AdminApplicationView.APPLICATION_STATUS_SELECTOR,
    ) as HTMLFormElement | null
    if (!statusSelectForm) {
      // If status tracking isn't enabled, there's nothing to do.
      return
    }
    const statusSelector = this._assertNotNull(
      statusSelectForm.querySelector('select') as HTMLSelectElement | null,
      'status selector',
    )

    // Remember the original value here since neither the 'change/input' events provide the
    // previous selected value. We need to reset the value when the confirmation is rejected.
    const originalSelectedValue = statusSelector.value
    statusSelector.addEventListener('change', (event) => {
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
    const relevantStatusModalTrigger = this._assertNotNull(
      statusModalTriggers.find((statusModalTrigger) => {
        return (
          selectedStatus ===
          statusModalTrigger.getAttribute(
            AdminApplicationView.CONFIRM_MODAL_FOR_DATA_ATTRIBUTE,
          )
        )
      }) as HTMLButtonElement | null,
      'confirmation modal button',
    )
    relevantStatusModalTrigger.click()
  }

  _assertNotNull<T>(value: T | null | undefined, description: string): T {
    if (value == null) {
      throw new Error(`Expected ${description} not to be null.`)
    }

    return value
  }
}

window.addEventListener('load', () => new AdminApplicationView())
