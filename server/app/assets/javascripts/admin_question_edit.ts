import {addEventListenerToElements, assertNotNull} from './util'
import {ModalController} from './modal'

class AdminQuestionEdit {
  constructor() {
    // Check for the modal to confirm we are on the question edit page
    const modal = document.getElementById('confirm-question-updates-modal')
    if (modal !== null) {
      this.addCustomModalClickHandler(modal)
    }

    const primaryApplicantInfoSection = document.getElementById(
      'primary-applicant-info',
    )
    if (primaryApplicantInfoSection !== null) {
      this.addUniversalToggleHandler(primaryApplicantInfoSection)
      this.addEnumeratorDropdownHandler(primaryApplicantInfoSection)
    }
  }

  addEnumeratorDropdownHandler(primaryApplicantInfoSection: HTMLElement) {
    addEventListenerToElements(
      '#question-enumerator-select',
      'input',
      (event: Event) => {
        const target = event.target as HTMLInputElement
        const setHidden = target.value !== ''
        primaryApplicantInfoSection.toggleAttribute('hidden', setHidden)
      },
    )
  }

  addUniversalToggleHandler(primaryApplicantInfoSection: HTMLElement) {
    const primaryApplicantInfoSubsections =
      primaryApplicantInfoSection.querySelectorAll(
        '.cf-primary-applicant-info-subsection',
      )
    const universalInput = document.getElementById(
      'universal-toggle-input',
    ) as HTMLInputElement
    addEventListenerToElements('#universal-toggle', 'click', () => {
      primaryApplicantInfoSubsections.forEach((subsection) => {
        const toggle = subsection.querySelector('.cf-toggle') as HTMLElement
        const input = toggle.querySelector(
          '.cf-toggle-hidden-input',
        ) as HTMLInputElement
        const universalAlert = subsection.querySelector(
          '.cf-primary-applicant-info-universal-alert',
        ) as HTMLElement
        const alreadySetAlert = subsection.querySelector(
          '.cf-primary-applicant-info-tag-already-set-alert',
        ) // May be null
        // Do not toggle things if this is hidden because the
        // "you've already set this action on a different question" alert is showing.
        if (alreadySetAlert === null) {
          // Unset the action when we unset universal
          // Because the universal input doesn't change until after the click event,
          // we're checking for true here.
          if (input.value === 'true' && universalInput.value === 'true') {
            toggle.click()
          }
          toggle.toggleAttribute('hidden')
        }
        universalAlert.toggleAttribute('hidden')
      })
    })
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
