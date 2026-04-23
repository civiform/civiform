import {ToastController} from '@/toast'
import {addEventListenerToElements} from '@/util'
import type {HtmxDetail} from 'htmx'

enum ProgramType {
  DEFAULT = 'CiviForm program',
  PRE_SCREENER_FORM = 'Pre-screener',
  EXTERNAL = 'External program',
}

class AdminPrograms {
  private static PROGRAM_CARDS_SELECTOR = '.cf-admin-program-card'
  private static PROGRAM_LINK_ATTRIBUTE = 'data-copyable-program-link'
  private static DISABLED_TEXT_CLASS = 'read-only:text-gray-500'
  private static DISABLED_BACKGROUND_CLASS = 'read-only:bg-gray-100'
  private static REPEATED_SET_LISTED_ENTITY_INPUT_ID = 'listed-entity-input'
  private static REPEATED_SET_ADMIN_ID_INPUT_ID = 'enumerator-admin-id-input'
  private static REPEATED_SET_QUESTION_TEXT_INPUT_ID = 'question-text-input'
  private static REPEATED_SET_PREVIOUS_ADMIN_ID_SUGGESTION_DATA_ATTRIBUTE =
    'previousAdminIdSuggestion'
  private static REPEATED_SET_PREVIOUS_QUESTION_TEXT_SUGGESTION_DATA_ATTRIBUTE =
    'previousQuestionTextSuggestion'

  static attachConfirmPreScreenerChangeListener() {
    addEventListenerToElements(
      '#confirm-pre-screener-change-button',
      'click',
      () => {
        const confirmationCheckbox = <HTMLInputElement>(
          document.querySelector('#confirmed-change-pre-screener-checkbox')
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
   * Attaches a change event listener to the program type selector to
   * manage the disabled state of related form elements.
   */
  static attachProgramTypeChangeListener() {
    // Listens for changes to the pre-screener checkbox.
    // TODO(#10363): This should be removed once EXTERNAL_PROGRAM_CARDS feature
    // flag is removed, which is handled by the next listener.
    addEventListenerToElements('#pre-screener-checkbox', 'click', () => {
      const preScreenerCheckbox = <HTMLInputElement>(
        document.querySelector('#pre-screener-checkbox')
      )

      const programType = preScreenerCheckbox.checked
        ? ProgramType.PRE_SCREENER_FORM
        : ProgramType.DEFAULT
      this.updateDisabledStateFields(programType)
    })

    // Listen for changes to the program type radio button, which is only used
    // when EXTERNAL_PROGRAM_CARDS feature is enabled.
    addEventListenerToElements('#program-type', 'click', () => {
      const preScreenerCheckbox = <HTMLInputElement>(
        document.querySelector('#pre-screener-program-option')
      )
      const externalProgramCheckbox = <HTMLInputElement>(
        document.querySelector('#external-program-option')
      )

      let programType = ProgramType.DEFAULT
      if (preScreenerCheckbox.checked) {
        programType = ProgramType.PRE_SCREENER_FORM
      } else if (externalProgramCheckbox.checked) {
        programType = ProgramType.EXTERNAL
      }
      this.updateDisabledStateFields(programType)
    })
  }

  /**
   * Updates the disabled state of form fields based on the selected program type
   *
   * @param programType - The type of program being configured
   */
  static updateDisabledStateFields(programType: ProgramType) {
    // Program categories
    const disableProgramCategories =
      programType === ProgramType.PRE_SCREENER_FORM
    this.updateUSWDSCheckboxesDisabledState(
      /* fieldSelectors= */ '[id^="checkbox-category"]',
      /* shouldDisable= */ disableProgramCategories,
    )

    // Program eligibility
    const disableProgramEligibility =
      programType === ProgramType.PRE_SCREENER_FORM ||
      programType === ProgramType.EXTERNAL
    this.updateUSWDSCheckboxesDisabledState(
      /* fieldSelectors= */ '[id^="program-eligibility"]',
      /* shouldDisable= */ disableProgramEligibility,
    )
    this.updateRequiredIndicatorState(
      /* fieldSelector= */ '#program-eligibility',
      /* shouldHide= */ disableProgramEligibility,
    )

    // Program external link
    const externalLink = document.getElementById(
      'program-external-link-input',
    ) as HTMLInputElement
    const disableExternalLink =
      programType === ProgramType.DEFAULT ||
      programType === ProgramType.PRE_SCREENER_FORM
    this.updateTextFieldElementDisabledState(
      /* fieldElement= */ externalLink,
      /* shouldDisable= */ disableExternalLink,
    )
    this.updateRequiredIndicatorState(
      /* fieldSelector= */ 'label[for="program-external-link-input"]',
      /* shouldHide= */ programType !== ProgramType.EXTERNAL,
    )

    // Notification preferences
    const disableNotificationPreferences = programType === ProgramType.EXTERNAL
    this.updateUSWDSCheckboxesDisabledState(
      /* fieldSelectors= */ '[id^="notification-preferences-email"]',
      /* shouldDisable= */ disableNotificationPreferences,
    )

    // Login only applications
    const diableLoginOnly = programType === ProgramType.EXTERNAL
    this.updateUSWDSCheckboxesDisabledState(
      /* fieldSelectors= */ '[id^="login-only-applications"]',
      /* shouldDisable= */ diableLoginOnly,
    )

    // Long program description
    const disableLongDescription =
      programType === ProgramType.PRE_SCREENER_FORM ||
      programType === ProgramType.EXTERNAL
    this.updateTextFieldSelectorsDisabledState(
      'textarea[id="program-display-description-textarea"]',
      disableLongDescription,
    )

    // Application steps
    const disableApplicationSteps =
      programType === ProgramType.PRE_SCREENER_FORM ||
      programType === ProgramType.EXTERNAL
    this.updateTextFieldSelectorsDisabledState(
      /* fieldSelectors= */ 'input[id^="apply-step"]',
      /* shouldDisable= */ disableApplicationSteps,
    )
    this.updateTextFieldSelectorsDisabledState(
      /* fieldSelectors= */ 'textarea[id^="apply-step"]',
      /* shouldDisable= */ disableApplicationSteps,
    )
    this.updateRequiredIndicatorState(
      /* fieldSelector= */ 'label[for="apply-step-1-title"]',
      /* shouldHide= */ disableApplicationSteps,
    )
    this.updateRequiredIndicatorState(
      /* fieldSelector= */ 'label[for="apply-step-1-description"]',
      /* shouldHide= */ disableApplicationSteps,
    )

    // Confirmation message
    const disableConfirmationMessage = programType === ProgramType.EXTERNAL
    this.updateTextFieldSelectorsDisabledState(
      /* fieldSelectors= */ 'textarea[id="program-confirmation-message-textarea"]',
      /* shouldDisable= */ disableConfirmationMessage,
    )
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
      fieldElement.readOnly = true
      fieldElement.classList.add(
        this.DISABLED_TEXT_CLASS,
        this.DISABLED_BACKGROUND_CLASS,
      )
    } else {
      fieldElement.disabled = false
      fieldElement.readOnly = false
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
  static updateRequiredIndicatorState(
    fieldSelector: string,
    shouldHide: boolean,
  ) {
    const labelElement = document.querySelector(fieldSelector)
    if (!labelElement) {
      return
    }

    const requiredSpan = labelElement.querySelector('.usa-hint--required')
    if (!requiredSpan) {
      return
    }

    if (shouldHide) {
      requiredSpan.classList.add('hidden')
    } else {
      requiredSpan.classList.remove('hidden')
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

  static attachEventListenerToHtmxSwap() {
    document.body.addEventListener('htmx:afterSwap', (e) => {
      const targetElement = e.detail.target
      if (targetElement.id === 'enumerator-setup') {
        if (document.getElementById('new-enumerator-question-form-errors')) {
          this.focusOnFirstEnumeratorFormField()
        } else {
          this.focusOnEnumeratorQuestionSection()
        }
      }
    })
  }

  static attachEventListenerToEnumeratorCreationMethod() {
    const radioButtons = document.querySelectorAll(
      'input[name="creation-method-option"]',
    )
    const enumerator_question_form = document.querySelector(
      '#new-enumerator-question-form',
    ) as HTMLElement
    const add_question_section = document.querySelector(
      '#add-question-section',
    ) as HTMLElement

    radioButtons.forEach((radio) => {
      radio.addEventListener('change', (event) => {
        // hide all sections
        enumerator_question_form.style.display = 'none'
        add_question_section.style.display = 'none'

        const target = event.target as HTMLInputElement

        if (target.value == 'create-new') {
          enumerator_question_form.style.display = 'block'
        } else {
          add_question_section.style.display = 'block'
        }
      })
    })
  }

  static focusOnEnumeratorQuestionSection() {
    const enumeratorSectionHeading = document.getElementById(
      'repeated-set-question-section-heading',
    )
    if (enumeratorSectionHeading) {
      enumeratorSectionHeading.focus()
    }
  }

  static focusOnFirstEnumeratorFormField() {
    const firstInputField = document.getElementById('listed-entity-input')
    if (firstInputField) {
      firstInputField.focus()
    }
  }

  static attachEventListenerToRepeatedSetFieldAutofill() {
    const listedEntityInputElement = document.getElementById(
      this.REPEATED_SET_LISTED_ENTITY_INPUT_ID,
    ) as HTMLInputElement | null
    if (!listedEntityInputElement) {
      return
    }

    listedEntityInputElement.addEventListener('input', () => {
      this.maybeAutofillRepeatedSetFormFields(listedEntityInputElement)
    })
  }

  static maybeAutofillRepeatedSetFormFields(
    listedEntityInput?: HTMLInputElement,
  ) {
    const listedEntity = listedEntityInput
      ? listedEntityInput
      : (document.getElementById(
          this.REPEATED_SET_LISTED_ENTITY_INPUT_ID,
        ) as HTMLInputElement | null)

    const adminIdInput = document.getElementById(
      this.REPEATED_SET_ADMIN_ID_INPUT_ID,
    ) as HTMLInputElement | null
    const questionTextInput = document.getElementById(
      this.REPEATED_SET_QUESTION_TEXT_INPUT_ID,
    ) as HTMLTextAreaElement | null

    if (!listedEntity || !adminIdInput || !questionTextInput) {
      return
    }

    const normalizedListedEntity = this.normalizeRepeatedSetEntity(
      listedEntity.value,
    )
    // Get i18n message templates from data attributes
    const adminIdTemplate = adminIdInput.dataset.adminIdAutofillTemplate ?? ''
    const questionTextTemplate =
      questionTextInput.dataset.questionTextAutofillTemplate ?? ''
    const suggestedAdminId =
      normalizedListedEntity.length > 0 && adminIdTemplate
        ? adminIdTemplate.replace('{0}', normalizedListedEntity)
        : ''
    const suggestedQuestionText =
      normalizedListedEntity.length > 0 && questionTextTemplate
        ? questionTextTemplate.replace('{0}', normalizedListedEntity)
        : ''

    this.maybeSetAutofillValue(
      adminIdInput,
      suggestedAdminId,
      this.REPEATED_SET_PREVIOUS_ADMIN_ID_SUGGESTION_DATA_ATTRIBUTE,
    )
    this.maybeSetAutofillValue(
      questionTextInput,
      suggestedQuestionText,
      this.REPEATED_SET_PREVIOUS_QUESTION_TEXT_SUGGESTION_DATA_ATTRIBUTE,
    )
  }

  static maybeSetAutofillValue(
    input: HTMLInputElement | HTMLTextAreaElement,
    suggestion: string,
    previousSuggestionDataAttribute: string,
  ) {
    const previousSuggestion =
      input.dataset[previousSuggestionDataAttribute] ?? ''

    // Only overwrite when untouched by admins (still equal to previous suggestion)
    // or when admins intentionally clear the field.
    if (input.value === '' || input.value === previousSuggestion) {
      input.value = suggestion
    }

    input.dataset[previousSuggestionDataAttribute] = suggestion
  }

  static normalizeRepeatedSetEntity(entity: string): string {
    return entity.trim().replace(/\s+/g, ' ').toLowerCase()
  }
}

export function init() {
  AdminPrograms.attachCopyProgramLinkListeners()
  AdminPrograms.attachConfirmPreScreenerChangeListener()
  AdminPrograms.attachProgramTypeChangeListener()
  AdminPrograms.attachEventListenersToEditTIButton()
  AdminPrograms.attachEventListenersToHideEditTiInPublicMode()
  AdminPrograms.attachEventListenersToHideEditTiInTIOnlyMode()
  AdminPrograms.attachEventListenersToHideEditTiInHiddenMode()
  AdminPrograms.attachEventListenerToHtmxSwap()
  AdminPrograms.attachEventListenerToEnumeratorCreationMethod()
  AdminPrograms.attachEventListenerToRepeatedSetFieldAutofill()
}
