class AdminApplicationView {
  private static APPLICATION_STATUS_SELECTOR =
    '.cf-program-admin-status-selector'
  private static CONFIRM_MODAL_ID = 'confirm-status-modal'
  private static CONFIRM_MODAL_TRIGGER_BUTTON_ID = 'confirm-status-modal-button'

  constructor() {
    this.registerStatusSelectorEventListener()
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

    // The original value is tracked since neither the 'change/input' events provide the previous
    // value prior to input.
    const originalSelectedValue = statusSelector.value
    statusSelector.addEventListener('change', (event) => {
      // The promise result is not awaited since the modal is responsible for updating the status
      // and we only care about showing it.
      this.showConfirmStatusChangeModal(statusSelector.value)
      // Reset the selection to its previous value in the case that the user cancels the
      // confirmation dialog.
      statusSelector.value = originalSelectedValue
    })
  }

  private async showConfirmStatusChangeModal(
    newValue: string,
  ): Promise<boolean> {
    const confirmModal = this._assertNotNull(
      document.getElementById(
        AdminApplicationView.CONFIRM_MODAL_ID,
      ) as HTMLElement | null,
      'confirmation modal',
    )
    // TODO(clouser): Update the dynamic status.
    const confirmModalContentToUpdate =
      confirmModal.querySelectorAll('[data-status-text]')
    if (confirmModalContentToUpdate.length > 0) {
      throw new Error('unexpected content')
    }

    const confirmModalButton = this._assertNotNull(
      document.getElementById(
        AdminApplicationView.CONFIRM_MODAL_TRIGGER_BUTTON_ID,
      ) as HTMLButtonElement | null,
      'confirmation modal trigger button',
    )
    confirmModalButton.click()
    console.log('showed modal')
    return true
  }

  _assertNotNull<T>(value: T | null | undefined, description: string): T {
    if (value == null) {
      throw new Error(`Expected ${description} not to be null.`)
    }

    return value
  }
}

window.addEventListener('load', () => new AdminApplicationView())
