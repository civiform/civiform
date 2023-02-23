import {addEventListenerToElements, assertNotNull} from './util'

class AdminProgramStatusesView {
  private static STATUS_CHANGE_FORM_SELECTOR = '.cf-status-change-form'

  constructor() {
    this.registerStatusUpdateFormSubmitListener()
  }

  private registerStatusUpdateFormSubmitListener() {
    addEventListenerToElements(
      AdminProgramStatusesView.STATUS_CHANGE_FORM_SELECTOR,
      'submit',
      (event: Event) => this.confirmStatusDefaultChange(event),
    )
  }

  private confirmStatusDefaultChange(event: Event) {
    const statusForm = event.target as HTMLFormElement

    const statusText = assertNotNull(
      (statusForm.querySelector('input[name="statusText"]') as HTMLInputElement)
        .value,
    )
    const dontShow = assertNotNull(statusForm.dataset.dontshow) === 'true'
    const messagePart = assertNotNull(statusForm.dataset.messagepart)
    const checked = assertNotNull(
      statusForm.querySelector(
        '.cf-set-default-status-checkbox',
      ) as HTMLInputElement,
    ).checked
    const confirmationMessage =
      'The default status will be updated ' +
      messagePart +
      statusText +
      '. Are you sure?'

    if (!dontShow && checked) {
      event.preventDefault()
      if (confirm(confirmationMessage)) {
        statusForm.submit()
      }
    }
  }
}

export function init() {
  new AdminProgramStatusesView()
}
