import {assertNotNull} from './util'
import {ModalController} from './modal'

class AdminQuestionEdit {
  constructor() {
    // Check for the universal questions toggle button to confirm we are on the question edit page with the UNIVERSAL_QUESTIONS feature flag turned on
    const toggleElement = document.getElementById(
      'universal-toggle-input',
    ) as HTMLInputElement
    if (toggleElement === null) {
      return
    }

    const modalContainer = assertNotNull(
      document.getElementById('modal-container'),
    )
    const modal = assertNotNull(
      document.getElementById('confirm-question-updates-modal'),
    )
    const modalTriggerButton = assertNotNull(
      document.getElementById('confirm-question-updates-modal-button'),
    )

    // Get the toggle value on page load so we can compare it to the toggle value on click
    const initialToggleValue = toggleElement.value

    // Remove the default event listener on the modal since we want to show it conditionally
    ModalController.abortSignal.abort()

    // Add a new click handler that checks if the toggle went from "on" to "off"
    modalTriggerButton.addEventListener('click', (e: Event) => {
      e.stopPropagation() // do we need this?

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
