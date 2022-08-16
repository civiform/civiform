class AdminApplicationView {
  private static APPLICATION_STATUS_SELECTOR =
    '.cf-program-admin-status-selector'

  constructor() {
    this.registerStatusSelectorEventListener()
  }

  private registerStatusSelectorEventListener() {
    const statusSelectFormEl = document.querySelector(
      AdminApplicationView.APPLICATION_STATUS_SELECTOR,
    ) as HTMLFormElement | null
    if (!statusSelectFormEl) {
      // If status tracking isn't enabled, there's nothing to do.
      return
    }
    const selectEl = this._assertNotNull(
      statusSelectFormEl.querySelector('select') as HTMLSelectElement | null,
      'status selector',
    )
    let previousVal: string
    selectEl.addEventListener('focusin', (event) => {
      previousVal = selectEl.value
    })
    selectEl.addEventListener('change', (event) => {
      // Show a confirmation dialog.
      if (!confirm('TODO(clouser): Figure out the correct message.')) {
        // Change only fires when the value is changed due to user
        // interaction, thus avoiding the event refiring as part
        // of setting "value" below.
        selectEl.value = previousVal
        return
      }
      statusSelectFormEl.submit()
    })
  }

  _assertNotNull<T>(value: T | null | undefined, description: string): T {
    if (value == null) {
      throw new Error(`Expected ${description} not to be null.`)
    }

    return value
  }
}

window.addEventListener('load', () => new AdminApplicationView())
