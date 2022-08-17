class AdminApplicationView {
  private static APPLICATION_STATUS_SELECTOR =
    '.cf-program-admin-status-selector'
  private static CONFIRM_MODAL_FOR_DATA_ATTRIBUTE =
    'data-status-update-confirm-for-status'

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

  private showConfirmStatusChangeModal(newValue: string) {
    const statusModalTriggers = Array.from(
      document.querySelectorAll(
        `[${AdminApplicationView.CONFIRM_MODAL_FOR_DATA_ATTRIBUTE}]`,
      ),
    )
    const relevantStatusModalTrigger = this._assertNotNull(
      statusModalTriggers.find((statusModalTrigger) => {
        return (
          newValue ===
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
