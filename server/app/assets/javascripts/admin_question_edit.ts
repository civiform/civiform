import {assertNotNull} from './util'
import {ModalController} from './modal'

class AdminQuestionEdit {
  constructor() {
    // Check for the modal to confirm we are on the question edit page
    const modal = document.getElementById('confirm-question-updates-modal')
    if (modal !== null) {
      this.addCustomModalClickHandler(modal)
    }
  }

  addCustomModalClickHandler(modal: HTMLElement) {
    const toggleElement = assertNotNull(
      document.getElementById('universal-toggle-input'),
    ) as HTMLInputElement
    const modalContainer = assertNotNull(
      document.getElementById('modal-container'),
    )
    const modalTriggerButton = assertNotNull(
      document.getElementById('confirm-question-updates-modal-button'),
    )

    // Get the toggle value on page load so we can compare it to the toggle value on click
    const initialToggleValue = toggleElement.value

    // Remove the default event listener on the modal since we want to show it conditionally
    ModalController.abortController.abort()

    // Add a new click handler that checks if the toggle went from "on" to "off"
    modalTriggerButton.addEventListener('click', () => {
      // Get the toggle value when the user clicks to update the question
      const currentToggleValue = toggleElement.value
      if (initialToggleValue === 'true' && currentToggleValue === 'false') {
        // If they are unsetting the universal question attribute, show a modal to confirm
        ModalController.showModal(modalContainer, modal)
      } else {
        // Otherwise, click the hidden "submit" button on the modal to submit the update
        const submitButton = assertNotNull(
          document.getElementById('accept-question-updates-button'),
        )
        submitButton.click()
      }
    })
  }
}

export function init() {
  new AdminQuestionEdit()
}
