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
   * Listens when the program type fieldset is selected and disables fields as applicable.
   */
  static attachProgramTypeChangeListener() {
    addEventListenerToElements('#program-type-fieldset', 'click', () => {
      // Get the program type radio elements.
      const commonIntakeType = <HTMLInputElement>(
        document.querySelector('#common-intake-type')
      )
      const externalProgramType = <HTMLInputElement>(
        document.querySelector('#external-program-type')
      )

      // Category elements are disabled when common intake type is selected.
      const programCategoryCheckboxes = document.querySelectorAll(
        '[id^="checkbox-category"]',
      )
      programCategoryCheckboxes.forEach((checkbox) => {
        const category = checkbox as HTMLInputElement
        if (commonIntakeType.checked) {
          category.disabled = true
          category.checked = false
        } else {
          category.disabled = false
        }
      })

      // Program eligibility options are disabled when external program type is
      // selected.
      const programEligibilityOptions = document.querySelectorAll(
        '[id^="program-eligibility"]',
      )
      console.log(programEligibilityOptions)
      programEligibilityOptions.forEach((checkbox) => {
        const category = checkbox as HTMLInputElement
        this.maybeDisableField(category, externalProgramType.checked)
      })

      // Email notifications checkbox is disabled when external program type is
      // selected.
      this.maybeDisableFieldById(
        'email-notifications-checkbox',
        externalProgramType.checked,
      )

      // Long description textarea is disabled when external program type is
      // selected, or common intake type is selected with North Star UI enabled.
      this.maybeDisableFieldById(
        'program-display-description-textarea',
        externalProgramType.checked ||
          (commonIntakeType.checked &&
            document.body.dataset.northstarEnabled === 'true'),
      )

      // Application steps are disabled when common intake type or external
      // program type is selected.
      this.maybeDisableApplicationSteps(
        'input[id^="apply-step"]',
        commonIntakeType.checked || externalProgramType.checked,
      )
      this.maybeDisableApplicationSteps(
        'textarea[id^="apply-step"]',
        commonIntakeType.checked || externalProgramType.checked,
      )
      // Remove the required indicator from the first application step
      if (commonIntakeType.checked || externalProgramType.checked) {
        const applicationStepOneDiv =
          document.querySelector('#apply-step-1-div')
        const requiredIndicators =
          applicationStepOneDiv?.querySelectorAll('span')
        requiredIndicators?.forEach((indicator) => {
          indicator.classList.add('hidden')
        })
      }

      // Confirmation message is disabled when external program is selected.
      this.maybeDisableFieldById(
        'program-confirmation-message-textarea',
        externalProgramType.checked,
      )
    })
  }

  static maybeDisableApplicationSteps(
    applicationStepsSelector: string,
    shouldDisable: boolean,
  ) {
    const applicationStepFields = document.querySelectorAll(
      applicationStepsSelector,
    )
    applicationStepFields.forEach((step) => {
      const applicationStepField = step as HTMLInputElement
      this.maybeDisableField(applicationStepField, shouldDisable)
    })
  }

  static maybeDisableFieldById(elementId: string, shouldDisable: boolean) {
    const element = document.getElementById(elementId) as HTMLInputElement
    this.maybeDisableField(element, shouldDisable)
  }

  static maybeDisableField(field: HTMLInputElement, shouldDisable: boolean) {
    if (shouldDisable) {
      field.disabled = true
      field.checked = false
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
  AdminPrograms.attachProgramTypeChangeListener()
  AdminPrograms.attachEventListenersToEditTIButton()
  AdminPrograms.attachEventListenersToHideEditTiInPublicMode()
  AdminPrograms.attachEventListenersToHideEditTiInTIOnlyMode()
  AdminPrograms.attachEventListenersToHideEditTiInHiddenMode()
}
