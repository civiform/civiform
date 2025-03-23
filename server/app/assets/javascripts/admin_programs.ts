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

  // When the common intake checkbox is selected,
  // the following fields should be disabled:
  // - program category checkboxes (disabled and unchecked)
  // - application steps
  // - long program description (only if northstar UI is enabled)
  static attachCommonIntakeChangeListener() {
    addEventListenerToElements('#common-intake-checkbox', 'click', () => {
      const commonIntakeCheckbox = <HTMLInputElement>(
        document.querySelector('#common-intake-checkbox')
      )

      const programCategoryCheckboxes = document.querySelectorAll(
        '[id^="checkbox-category"]',
      )
      programCategoryCheckboxes.forEach((checkbox) => {
        const category = checkbox as HTMLInputElement
        if (commonIntakeCheckbox.checked) {
          category.disabled = true
          category.checked = false
        } else {
          category.disabled = false
        }
      })

      const longDescription = document.getElementById(
        'program-display-description-textarea',
      ) as HTMLInputElement
      const northStarUiEnabled =
        longDescription.dataset.northstarEnabled === 'true'
      this.maybeDisableField(
        longDescription,
        commonIntakeCheckbox.checked && northStarUiEnabled,
      )

      const applicationStepTitles = document.querySelectorAll(
        'input[id^="apply-step"]',
      )
      const applicationStepDescriptions = document.querySelectorAll(
        'textarea[id^="apply-step"]',
      )
      this.maybeDisableApplicationSteps(
        applicationStepTitles,
        commonIntakeCheckbox,
      )
      this.maybeDisableApplicationSteps(
        applicationStepDescriptions,
        commonIntakeCheckbox,
      )
      // remove the required indicator from the first application step
      const applicationStepOneDiv = document.querySelector('#apply-step-1-div')
      if (commonIntakeCheckbox.checked) {
        const requiredIndicators =
          applicationStepOneDiv?.querySelectorAll('span')
        requiredIndicators?.forEach((indicator) => {
          indicator.classList.add('hidden')
        })
      }
    })
  }

  static maybeDisableApplicationSteps(
    applicationStepFields: NodeListOf<Element>,
    commonIntakeCheckbox: HTMLInputElement,
  ) {
    applicationStepFields.forEach((step) => {
      const applicationStepField = step as HTMLInputElement
      this.maybeDisableField(applicationStepField, commonIntakeCheckbox.checked)
    })
  }

  static maybeDisableField(field: HTMLInputElement, shouldDisable: boolean) {
    if (shouldDisable) {
      field.disabled = true
      field.classList.add(
        this.DISABLED_TEXT_CLASS,
        this.DISABLED_BACKGROUND_CLASS,
      )
    } else {
      field.disabled = false
    }
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
