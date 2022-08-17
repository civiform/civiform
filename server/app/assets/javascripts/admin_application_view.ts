class AdminApplicationView {
  private static APPLICATION_STATUS_SELECTOR =
    '.cf-program-admin-status-selector'

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

    // The original value is tracked neither the 'change/input' events provide the previous
    // value prior to input.
    const originalSelectedValue = statusSelector.value
    statusSelector.addEventListener('change', (event) => {
      if (!this.confirmStatusChange(statusSelector.value)) {
        // Change only fires when the value is changed due to user interaction, thus avoiding the
        // event refiring as part of setting "value" below.
        statusSelector.value = originalSelectedValue
        return
      }
      // No explicit submit button is shown. After the user has confirmed the change, the form is
      // explicitly submitted.
      statusSelectForm.submit()
    })
  }

  private confirmStatusChange(newValue: string): boolean {
    const message = `Are you sure you want to set the status for this application to ${newValue}?`
    return confirm(message)
  }

  _assertNotNull<T>(value: T | null | undefined, description: string): T {
    if (value == null) {
      throw new Error(`Expected ${description} not to be null.`)
    }

    return value
  }
}

window.addEventListener('load', () => new AdminApplicationView())
