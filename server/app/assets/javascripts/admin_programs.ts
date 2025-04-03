import {ToastController} from './toast'
import {addEventListenerToElements} from './util'

class AdminPrograms {
  private static PROGRAM_CARDS_SELECTOR = '.cf-admin-program-card'
  private static PROGRAM_LINK_ATTRIBUTE = 'data-copyable-program-link'
  private static DISABLED_TEXT_CLASS = 'read-only:text-gray-500'
  private static DISABLED_BACKGROUND_CLASS = 'read-only:bg-gray-100'

  static attachConfirmCommonIntakeChangeListener() {
    addEventListenerToElements(
      '#confirm-common-intake-change-button',
      'click',
      () => {
        const confirmationCheckbox = <HTMLInputElement>(
          document.querySelector('#confirmed-change-common-intake-checkbox')
        )
        if (!confirmationCheckbox) {
          return
        }
        confirmationCheckbox.value = 'true'
        confirmationCheckbox.checked = true
      },
    )
  }

  /**
   * Attaches a change event listener to the common intake checkbox to manage
   * the disabled state of related form elements. When the common intake
   * checkbox is checked, the following fields are disabled:
   * 1. Program categories
   * 2. Long program description, if NorthStar UI is enabled
   * 3. All application steps
   *
   * When the checkbox is unchecked, all these elements are re-enabled and
   * required indicators are shown again.
   */
  static attachCommonIntakeChangeListener() {
    addEventListenerToElements('#common-intake-checkbox', 'click', () => {
      const commonIntakeCheckbox = <HTMLInputElement>(
        document.querySelector('#common-intake-checkbox')
      )

      // Program categories
      this.updateUSWDSCheckboxesDisabledState(
        /* fieldSelectors= */ '[id^="checkbox-category"]',
        /* shouldDisable= */ commonIntakeCheckbox.checked,
      )

      // Program eligibility
      this.updateUSWDSCheckboxesDisabledState(
        /* fieldSelectors= */ '[id^="program-eligibility"]',
        /* shouldDisable= */ commonIntakeCheckbox.checked,
      )
      this.hideRequiredIndicators(
        /* fieldSelector= */ '#program-eligibility',
        /* shouldHide= */ commonIntakeCheckbox.checked,
      )

      // Long program description
      const longDescription = document.getElementById(
        'program-display-description-textarea',
      ) as HTMLInputElement
      const northStarUiEnabled =
        longDescription.dataset.northstarEnabled === 'true'
      this.updateTextFieldElementDisabledState(
        /* fieldElement= */ longDescription,
        /* shouldDisable= */ commonIntakeCheckbox.checked && northStarUiEnabled,
      )

      // Application steps
      this.updateTextFieldSelectorsDisabledState(
        /* fieldSelectors= */ 'input[id^="apply-step"]',
        /* shouldDisable= */ commonIntakeCheckbox.checked,
      )
      this.updateTextFieldSelectorsDisabledState(
        /* fieldSelectors= */ 'textarea[id^="apply-step"]',
        /* shouldDisable= */ commonIntakeCheckbox.checked,
      )
      // Step #1 should hide required indicator if it's a common intake form.
      this.hideRequiredIndicators(
        /* fieldSelector= */ '#apply-step-1-div',
        /* shouldHide= */ commonIntakeCheckbox.checked,
      )
    })
  }

  /**
   * Updates the disabled state for multiple text fields matching the provided selector.
   *
   * @param fieldSelectors - CSS selector string to identify the text fields to update
   * @param shouldDisable - Boolean indicating whether to disable (true) or enable (false) the fields
   */
  static updateTextFieldSelectorsDisabledState(
    fieldSelectors: string,
    shouldDisable: boolean,
  ) {
    const textFields = document.querySelectorAll(fieldSelectors)
    textFields.forEach((field) => {
      const fieldElement = field as HTMLInputElement
      this.updateTextFieldElementDisabledState(fieldElement, shouldDisable)
    })
  }

  /**
   * Updates the disabled state for a single text field element.
   *
   * @param fieldElement - The HTML input element to update
   * @param shouldDisable - Boolean indicating whether to disable (true) or enable (false) the field
   */
  static updateTextFieldElementDisabledState(
    fieldElement: HTMLInputElement,
    shouldDisable: boolean,
  ) {
    if (shouldDisable) {
      fieldElement.disabled = true
      fieldElement.classList.add(
        this.DISABLED_TEXT_CLASS,
        this.DISABLED_BACKGROUND_CLASS,
      )
    } else {
      fieldElement.disabled = false
    }
  }

  /**
   * Updates the disabled state for USWDS checkboxes matching the provided selector.
   * When disabling checkboxes, also unchecks them to prevent submitting their values.
   *
   * @param fieldSelectors - CSS selector string to identify the checkboxes to update
   * @param shouldDisable - Boolean indicating whether to disable (true) or enable (false) the checkboxes
   */
  static updateUSWDSCheckboxesDisabledState(
    fieldSelectors: string,
    shouldDisable: boolean,
  ) {
    const checkboxes = document.querySelectorAll(fieldSelectors)
    checkboxes.forEach((checkbox) => {
      const checkboxElement = checkbox as HTMLInputElement
      if (shouldDisable) {
        checkboxElement.disabled = true
        checkboxElement.checked = false
      } else {
        checkboxElement.disabled = false
      }
    })
  }

  /**
   * Adds or removes the required indicator for a field
   *
   * @param {string} fieldSelector - The selector for the field
   * @param {boolean} shouldHide - Whether to show or hide the required indicator
   */
  static hideRequiredIndicators(fieldSelector: string, shouldHide: boolean) {
    const field = document.querySelector(fieldSelector)
    const requiredIndicators = field?.querySelectorAll('span')
    requiredIndicators?.forEach((indicator) => {
      if (shouldHide) {
        indicator.classList.add('hidden')
      } else {
        indicator.classList.remove('hidden')
      }
    })
  }

  static attachEventListenersToEditTIButton() {
    addEventListenerToElements(
      '#program-display-mode-select-ti-only',
      'click',
      () => {
        const tiSelect = <HTMLInputElement>document.querySelector('#TiList')
        if (tiSelect.hidden) tiSelect.hidden = false
        else {
          tiSelect.hidden = true
        }
      },
    )
  }
  static attachEventListenersToHideEditTiInPublicMode() {
    addEventListenerToElements('#program-display-mode-public', 'click', () => {
      const tiSelect = <HTMLInputElement>document.querySelector('#TiList')
      if (!tiSelect.hidden) tiSelect.hidden = true
    })
  }
  static attachEventListenersToHideEditTiInTIOnlyMode() {
    addEventListenerToElements('#program-display-mode-ti-only', 'click', () => {
      const tiSelect = <HTMLInputElement>document.querySelector('#TiList')
      if (!tiSelect.hidden) tiSelect.hidden = true
    })
  }
  static attachEventListenersToHideEditTiInHiddenMode() {
    addEventListenerToElements('#program-display-mode-hidden', 'click', () => {
      const tiSelect = <HTMLInputElement>document.querySelector('#TiList')
      if (!tiSelect.hidden) tiSelect.hidden = true
    })
  }
  static attachCopyProgramLinkListeners() {
    const withCopyableProgramLink = Array.from(
      document.querySelectorAll(
        `${AdminPrograms.PROGRAM_CARDS_SELECTOR} [${AdminPrograms.PROGRAM_LINK_ATTRIBUTE}]`,
      ),
    )
    withCopyableProgramLink.forEach((el) => {
      const programLink = el.getAttribute(AdminPrograms.PROGRAM_LINK_ATTRIBUTE)
      if (!programLink) {
        console.warn(
          `Empty ${AdminPrograms.PROGRAM_LINK_ATTRIBUTE} for element`,
        )
        return
      }
      el.addEventListener('click', () => {
        void AdminPrograms.copyProgramLinkToClipboard(programLink)
      })
    })
  }

  /**
   * Attempts to copy the given content to the clipboard.
   * @param {string} content
   * @return {Promise<boolean>} indicating whether the content was copied to the clipboard
   */
  static async tryCopyToClipboard(content: string): Promise<boolean> {
    if (!window.navigator['clipboard']) {
      return false
    }
    try {
      await window.navigator['clipboard'].writeText(content)
      return true
    } catch {
      return false
    }
  }

  static async copyProgramLinkToClipboard(programLink: string) {
    const succeeded = await AdminPrograms.tryCopyToClipboard(programLink)
    if (succeeded) {
      ToastController.showToastMessage({
        id: `program-link-${Math.random()}`,
        content: 'Program link copied to clipboard',
        duration: 3000,
        type: 'success',
        condOnStorageKey: null,
        canDismiss: true,
        canIgnore: false,
      })
    } else {
      ToastController.showToastMessage({
        id: `program-link-${Math.random()}`,
        content: `Could not copy program link to clipboard: ${programLink}`,
        duration: -1,
        type: 'warning',
        condOnStorageKey: null,
        canDismiss: true,
        canIgnore: false,
      })
    }
  }
}

export function init() {
  AdminPrograms.attachCopyProgramLinkListeners()
  AdminPrograms.attachConfirmCommonIntakeChangeListener()
  AdminPrograms.attachCommonIntakeChangeListener()
  AdminPrograms.attachEventListenersToEditTIButton()
  AdminPrograms.attachEventListenersToHideEditTiInPublicMode()
  AdminPrograms.attachEventListenersToHideEditTiInTIOnlyMode()
  AdminPrograms.attachEventListenersToHideEditTiInHiddenMode()
}
