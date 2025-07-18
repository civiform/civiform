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

    this.addDateValidationHandlers()
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
        const notUniversalAlert = subsection.querySelector(
          '.cf-pai-not-universal-alert',
        )
        const tagSetAlert = subsection.querySelector('.cf-pai-tag-set-alert')
        const tagSetNotUniversalAlert = subsection.querySelector(
          '.cf-pai-tag-set-not-universal-alert',
        )
        const togglediv = assertNotNull(
          subsection.querySelector('.cf-toggle-div'),
        ) as HTMLDivElement
        const togglebutton = assertNotNull(
          togglediv.querySelector('.cf-toggle-button'),
        ) as HTMLButtonElement
        const input = assertNotNull(
          togglediv.querySelector('.cf-toggle-hidden-input'),
        ) as HTMLInputElement
        if (notUniversalAlert !== null) {
          // Tag is not already set on another question, so we are
          // showing/hiding the toggle and alert.

          // Unset the PAI toggle when we unset universal.
          // Because the universal input doesn't change until after the click event,
          // we're checking for true here.
          if (input.value === 'true' && universalInput.value === 'true') {
            togglebutton.click()
          }
          togglediv.toggleAttribute('hidden')
          notUniversalAlert.toggleAttribute('hidden')
        } else {
          // Tag is set on another question, so we're just deciding which
          // alert to show, and the toggle will remain hidden.
          assertNotNull(tagSetAlert).toggleAttribute('hidden')
          assertNotNull(tagSetNotUniversalAlert).toggleAttribute('hidden')
        }
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

  addDateValidationHandlers() {
    // Add min date handler if date validation settings are present
    const minDateTypeDropdown = document.getElementById('min-date-type')
    const minCustomDatePicker = document.getElementById(
      'min-custom-date-fieldset',
    )
    if (minDateTypeDropdown !== null && minCustomDatePicker !== null) {
      this.addDateTypeDropdownHandler(
        minDateTypeDropdown,
        minCustomDatePicker,
        'min-custom-date',
      )
    }
    // Add max date handler if date validation settings are present
    const maxDateTypeDropdown = document.getElementById('max-date-type')
    const maxCustomDatePicker = document.getElementById(
      'max-custom-date-fieldset',
    )
    if (maxDateTypeDropdown !== null && maxCustomDatePicker !== null) {
      this.addDateTypeDropdownHandler(
        maxDateTypeDropdown,
        maxCustomDatePicker,
        'max-custom-date',
      )
    }
  }

  /**
   * Handles showing the custom date picker if custom date type is selected. Hides the date picker and clears date picker values otherwise.
   */
  addDateTypeDropdownHandler(
    dateTypeDropdown: HTMLElement,
    datePicker: HTMLElement,
    idPrefix: string,
  ) {
    dateTypeDropdown.addEventListener('change', (event: Event) => {
      const target = event.target as HTMLInputElement
      const dateTypeValue: string = target.value
      // Show date picker iff type is custom
      datePicker.toggleAttribute('hidden', dateTypeValue !== 'CUSTOM')

      // Clear date picker values if type is not custom
      if (dateTypeValue !== 'CUSTOM') {
        ;(
          document.getElementById(idPrefix + '-day') as HTMLInputElement
        ).value = ''
        ;(
          document.getElementById(idPrefix + '-month') as HTMLInputElement
        ).value = ''
        ;(
          document.getElementById(idPrefix + '-year') as HTMLInputElement
        ).value = ''
      }
    })
  }
}

export function init() {
  new AdminQuestionEdit()
}
