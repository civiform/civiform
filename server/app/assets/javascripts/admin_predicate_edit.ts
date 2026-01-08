import {HtmxAfterSwapEvent} from './htmx_request'
import {addEventListenerToElements, assertNotNull} from './util'

export class AdminPredicateEdit {
  // Set in server/app/views/admin/programs/EditPredicatePageView.html
  static NODE_OPERATOR_SELECT_ID = 'predicate-operator-node-select'
  static NODE_OPERATOR_SELECT_NULL_STATE_ID =
    'predicate-operator-node-select-null-state'
  // Set in server/app/views/admin/programs/predicates/EditSubconditionFragment.html
  static QUESTION_ID_SUFFIX: string = '-question'
  static ARIA_ANNOUNCE_ID_SUFFIX: string = '-ariaAnnounce'
  // Set in server/app/views/admin/programs/predicates/PredicateValuesInputFragment.html
  static VALUE_INPUT_ID_SUFFIX: string = '-value'
  static VALUE_INPUT_HINT_ID_SUFFIX: string = '-valueHintText'
  static FIRST_VALUE_INPUT_GROUP_ID_SUFFIX: string = '-firstValueGroup'
  static SECOND_VALUE_INPUT_ID_SUFFIX: string = '-secondValue'
  static SECOND_VALUE_INPUT_GROUP_ID_SUFFIX: string = '-secondValueGroup'
  static SUBCONDITION_LIST_ID_REGEX =
    /^predicate-condition-(\d+)-subcondition-list$/

  static CSV_OPERATORS: string[] = ['IN', 'NOT_IN']

  static INITIAL_PREDICATE_FORM_STATE: string

  static onHtmxAfterSwap(event: HtmxAfterSwapEvent): void {
    const targetId: string = event.target.id
    // Update for changes to 'subcondition-container', and also refreshes of condition lists.
    // The predicate list refreshes occur when a condition is deleted.
    if (
      event.target.classList.contains('subcondition-container') ||
      targetId === 'predicate-conditions-list' ||
      this.SUBCONDITION_LIST_ID_REGEX.test(targetId)
    ) {
      // Remove existing listeners and bind to new ones after the swap
      // replaces the html to ensure there's only one per element instead of
      // appending after each swap.
      document
        .querySelectorAll<HTMLSelectElement>('.cf-predicate-scalar-select')
        .forEach((dropdown: HTMLSelectElement) => {
          dropdown.removeEventListener(
            'change',
            this.onScalarDropdownChange.bind(this),
          )
          dropdown.addEventListener(
            'change',
            this.onScalarDropdownChange.bind(this),
          )
        })
      document
        .querySelectorAll<HTMLSelectElement>('.cf-predicate-operator-select')
        .forEach((dropdown: HTMLSelectElement) => {
          dropdown.removeEventListener(
            'change',
            this.onOperatorDropdownChange.bind(this),
          )
          dropdown.addEventListener(
            'change',
            this.onOperatorDropdownChange.bind(this),
          )
        })

      // Trigger change to update operators based on the current scalar selected.
      Array.from(
        document.querySelectorAll('.cf-predicate-scalar-select select'),
      ).forEach((el) => {
        const event = new CustomEvent('change', {bubbles: true})
        el.dispatchEvent(event)
      })

      // Trigger change to update values inputs based on the current operator selected.
      Array.from(
        document.querySelectorAll('.cf-predicate-operator-select select'),
      ).forEach((el) => {
        const event = new CustomEvent('change', {bubbles: true})
        el.dispatchEvent(event)
      })
    }
    AdminPredicateEdit.showOrHideDeleteAllConditionsButton()
    AdminPredicateEdit.showNodeOperatorSelectOrNullState()
    AdminPredicateEdit.focusSubconditionAndTriggerAriaAnnouncement()
  }

  onPageLoad(): void {
    const initialFormState = AdminPredicateEdit.getPredicateFormState()
    if (initialFormState) {
      AdminPredicateEdit.INITIAL_PREDICATE_FORM_STATE = initialFormState
    }

    AdminPredicateEdit.showOrHideDeleteAllConditionsButton()

    addEventListenerToElements(
      '.cf-predicate-scalar-select',
      'change',
      AdminPredicateEdit.onScalarDropdownChange.bind(this),
    )
    addEventListenerToElements(
      '.cf-predicate-operator-select',
      'change',
      AdminPredicateEdit.onOperatorDropdownChange.bind(this),
    )
    addEventListenerToElements(
      '#predicate-form',
      'submit',
      AdminPredicateEdit.onPredicateFormSubmit.bind(this),
    )

    // Trigger change to update operators based on the current scalar selected.
    Array.from(
      document.querySelectorAll('.cf-predicate-scalar-select select'),
    ).forEach((el) => {
      const event = new CustomEvent('change', {bubbles: true})
      el.dispatchEvent(event)
    })

    // Trigger change to update values inputs based on the current operator selected.
    Array.from(
      document.querySelectorAll('.cf-predicate-operator-select select'),
    ).forEach((el) => {
      const event = new CustomEvent('change', {bubbles: true})
      el.dispatchEvent(event)
    })
  }

  private static onScalarDropdownChange(event: Event): void {
    AdminPredicateEdit.handleScalarChange(event.target as HTMLSelectElement)
  }

  private static handleScalarChange(scalarDropdown: HTMLSelectElement) {
    // Get the type of scalar currently selected.
    const selectedScalarType =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type
    const selectedScalarValue =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].value

    this.filterOperators(
      scalarDropdown,
      selectedScalarType,
      selectedScalarValue,
    )
  }

  private static onOperatorDropdownChange(event: Event): void {
    AdminPredicateEdit.handleOperatorChange(event.target as HTMLSelectElement)
  }

  private static handleOperatorChange(operatorDropdown: HTMLSelectElement) {
    // Get the type of operator currently selected.
    const selectedOperatorValue =
      operatorDropdown.options[operatorDropdown.options.selectedIndex].value
    if (!selectedOperatorValue) {
      return
    }

    // Get the second value input group associated with this operator.
    const conditionId = operatorDropdown.getAttribute('data-condition-id')
    const subconditionId = operatorDropdown.getAttribute('data-subcondition-id')
    if (!conditionId || !subconditionId) {
      return
    }
    // Construct a "base ID" for the value elements.
    const valueBaseId = `condition-${conditionId}-subcondition-${subconditionId}`

    AdminPredicateEdit.manageValueInputVisibility(
      selectedOperatorValue,
      valueBaseId,
    )
    AdminPredicateEdit.manageValueInputFiltering(
      selectedOperatorValue,
      valueBaseId,
    )
  }

  private static onPredicateFormSubmit(event: SubmitEvent) {
    // If this is a submit, do nothing and let it go through.
    // If this is a cancel, handle submit manually.
    if (event.submitter && event.submitter.id === 'cancel-predicate-edit') {
      event.preventDefault()
      AdminPredicateEdit.confirmExitWithoutSaving(
        event.target as HTMLFormElement,
        event.submitter as HTMLButtonElement,
      )
    }
  }

  private static confirmExitWithoutSaving(
    predicateForm: HTMLFormElement,
    cancelButton: HTMLButtonElement,
  ) {
    const currentPredicateState = AdminPredicateEdit.getPredicateFormState()

    // Check the initial form state against the current form state.
    // If they're equal, do nothing and cancel.
    // If there have been changes, show a confirmation dialog.
    if (
      currentPredicateState !== AdminPredicateEdit.INITIAL_PREDICATE_FORM_STATE
    ) {
      const confirmationMessage =
        cancelButton.getAttribute('data-cancel-dialog')
      if (!confirmationMessage) {
        return
      }

      if (window.confirm(confirmationMessage)) {
        predicateForm.action = cancelButton.formAction
        predicateForm.submit()
      } else {
        return
      }
    }

    predicateForm.action = cancelButton.formAction
    predicateForm.submit()
  }

  /**
   * Depending on whether the user has added conditions in the predicate screen:
   *    * If yes, then show the normal "Applicant is eligible / Screen is visible if any/all conditions are true" text
   *    * If no, show the null state text.
   */
  private static showNodeOperatorSelectOrNullState(): void {
    const nodeOperatorSelect = document.getElementById(
      AdminPredicateEdit.NODE_OPERATOR_SELECT_ID,
    )
    if (nodeOperatorSelect) {
      const nodeOperatorSelectNullState = assertNotNull(
        document.getElementById(
          AdminPredicateEdit.NODE_OPERATOR_SELECT_NULL_STATE_ID,
        ),
      )
      if (document.querySelector('#condition-1')) {
        nodeOperatorSelect.hidden = false
        nodeOperatorSelectNullState.hidden = true
      } else {
        nodeOperatorSelect.hidden = true
        nodeOperatorSelectNullState.hidden = false
      }
    }
  }

  private static focusSubconditionAndTriggerAriaAnnouncement(): void {
    // Find which subcondition has autofocus set
    const focusedSubconditionQuestion: HTMLElement | null =
      document.querySelector(
        '.cf-predicate-question-select[data-should-autofocus="true"]',
      )
    if (focusedSubconditionQuestion === null) {
      return
    }

    const ariaAnnounceElementId = focusedSubconditionQuestion.id.replace(
      this.QUESTION_ID_SUFFIX,
      this.ARIA_ANNOUNCE_ID_SUFFIX,
    )
    const ariaAnnounceElement: HTMLElement | null = document.querySelector(
      `#${ariaAnnounceElementId}`,
    )
    if (ariaAnnounceElement === null) {
      return
    }

    // Focus the question dropdown of the desired subcondition
    focusedSubconditionQuestion.focus()

    // If we want an aria announcement here, set the text of the aria-live region after a short delay.
    // This update will trigger screen readers to read the text.
    if (ariaAnnounceElement.getAttribute('data-should-announce') === 'true') {
      setTimeout(function () {
        ariaAnnounceElement.textContent =
          ariaAnnounceElement.getAttribute('data-announce-text')
      }, 1000)
    }
  }

  /**
   * Manage visibility of value elements, depending on the currently selected operator.
   * This is used for showing/hiding the second value input for operators that require two values (e.g. BETWEEN),
   * and for showing/hiding the hint text for CSV input operators (e.g. IN, NOT_IN).
   *    @param {string} selectedOperatorValue: The currently selected operator.
   *    @param {string} valueBaseId: The base ID for the value elements. Used to find the correct elements. Format: condition-<conditionId>-subcondition-<subconditionId>
   */
  private static manageValueInputVisibility(
    selectedOperatorValue: string,
    valueBaseId: string,
  ) {
    if (!selectedOperatorValue) {
      return
    }

    AdminPredicateEdit.manageSecondValueInputVisibility(
      valueBaseId,
      selectedOperatorValue,
    )
    AdminPredicateEdit.manageValueInputHintVisibility(
      valueBaseId,
      selectedOperatorValue,
    )
  }

  /**
   * Manage filtering of value input types, depending on the currently selected operator and question type.
   * Some question types require different input types for certain operators.
   * E.g. date questions use date inputs for most operators, but use number inputs for age-based operators.
   *    @param {string} selectedOperatorValue: The currently selected operator.
   *    @param {string} valueBaseId: The base ID for the value elements. Used to find the correct elements. Format: condition-<conditionId>-subcondition-<subconditionId>
   */
  private static manageValueInputFiltering(
    selectedOperatorValue: string,
    valueBaseId: string,
  ) {
    const firstValueInputGroupId =
      valueBaseId + AdminPredicateEdit.FIRST_VALUE_INPUT_GROUP_ID_SUFFIX
    const secondValueGroupId =
      valueBaseId + AdminPredicateEdit.SECOND_VALUE_INPUT_GROUP_ID_SUFFIX

    // Find the HTML elements that are shared across question types:
    // defaultInput is the first input of the default input type (e.g. date-type for dates)
    // csvInput is the text field for multi-value operators (IN, NOT_IN)
    const defaultInputContainer = document.querySelector(
      `#${firstValueInputGroupId} [data-default-input-type][data-first-input]`,
    ) as HTMLElement | undefined

    // defaultInputContainer only exists for question types with multiple values inputs.
    // Return early if it's not found.
    if (!defaultInputContainer) {
      return
    }

    const defaultInputField = assertNotNull(
      defaultInputContainer.querySelector('input.usa-input'),
    ) as HTMLElement
    const csvInputContainer = document.querySelector(
      `#${firstValueInputGroupId} [data-csv-input-type]`,
    ) as HTMLElement | undefined

    // For question types that support CSV operators
    // Depending on the current operator, hide/show the csv input
    if (
      defaultInputField.hasAttribute('data-email-value') ||
      defaultInputField.hasAttribute('data-number-value')
    ) {
      this.filterCsvQuestionVisibleInputs(
        selectedOperatorValue,
        defaultInputContainer,
        assertNotNull(csvInputContainer),
      )
    }

    // For date values
    // Depending on the currently selected operator, filter visible input fields
    // Date operators vs. age operators vs. csv operators use different input fields.
    if (defaultInputField.hasAttribute('data-date-value')) {
      const ageInputContainer = assertNotNull(
        document.querySelector(
          `#${firstValueInputGroupId} [data-age-input-type][data-first-input]`,
        ),
      ) as HTMLElement
      const secondDateInputContainer = assertNotNull(
        document.querySelector(
          `#${secondValueGroupId} [data-default-input-type]`,
        ),
      ) as HTMLElement
      const secondAgeInputContainer = assertNotNull(
        document.querySelector(`#${secondValueGroupId} [data-age-input-type]`),
      ) as HTMLElement
      this.filterDateQuestionVisibleInputs(
        selectedOperatorValue,
        defaultInputContainer,
        ageInputContainer,
        secondDateInputContainer,
        secondAgeInputContainer,
        csvInputContainer!,
      )
    }
  }

  /**
   * Hide or show the second value input, depending on the currently selected operator.
   * For operators requring two values (e.g. BETWEEN), show the second value input. Otherwise, the second value input is hidden and disabled.
   *    @param {string} valueBaseId: The base ID of the second value element. Used to find the correct element. Format: condition-<conditionId>-subcondition-<subconditionId>
   *    @param {string} selectedOperatorValue: The currently selected operator.
   */
  private static manageSecondValueInputVisibility(
    valueBaseId: string,
    selectedOperatorValue: string,
  ) {
    // Get the input group (for visibility -- this is the surrounding div)
    const secondValueInputGroupId =
      valueBaseId + AdminPredicateEdit.SECOND_VALUE_INPUT_GROUP_ID_SUFFIX
    const secondValueInputGroup = document.getElementById(
      secondValueInputGroupId,
    )
    if (!secondValueInputGroup) {
      return
    }

    // Show or hide the second value input based on the selected operator.
    // Currently only BETWEEN operators require a second value.
    if (
      selectedOperatorValue === 'BETWEEN' ||
      selectedOperatorValue === 'AGE_BETWEEN'
    ) {
      AdminPredicateEdit.enableAndShowAll([secondValueInputGroup])
    } else {
      AdminPredicateEdit.disableAndHideAll([secondValueInputGroup])
    }
  }

  /**
   * Hide or show the hint text for multiple values input, depending on the operator type
   * selected. Currently only the IN and NOT_IN operators use this hint.
   *    @param {string} valueBaseId: The base ID for the value field. Used to find the hint element. Format: condition-<conditionId>-subcondition-<subconditionId>
   *    @param {string} selectedOperatorValue: The currently selected operator.
   */
  private static manageValueInputHintVisibility(
    valueBaseId: string,
    selectedOperatorValue: string,
  ) {
    const valueInputHintId =
      valueBaseId + AdminPredicateEdit.VALUE_INPUT_HINT_ID_SUFFIX
    const valueInputHint = document.getElementById(valueInputHintId)
    if (!valueInputHint) {
      return
    }

    // Show or hide the value input hint based on the selected operator.
    if (AdminPredicateEdit.CSV_OPERATORS.includes(selectedOperatorValue)) {
      valueInputHint.hidden = false
    } else {
      valueInputHint.hidden = true
    }
  }
  /**
   * Filter the operators available for each scalar type based on the current scalar selected.
   *   @param {HTMLSelectElement} scalarDropdown The element to filter the operators for.
   *   @param {string} selectedScalarType The type of the selected option.
   *   @param {string} selectedScalarValue The value of the selected option.
   */
  private static filterOperators(
    scalarDropdown: HTMLSelectElement,
    selectedScalarType: string | null | undefined,
    selectedScalarValue: string | null,
  ) {
    const operatorDropdownId = scalarDropdown.getAttribute(
      'data-operator-target-id',
    )
    if (!operatorDropdownId) {
      return
    }
    const operatorDropdown = document.getElementById(
      operatorDropdownId,
    ) as HTMLSelectElement
    if (!operatorDropdown) {
      return
    }

    Array.from(operatorDropdown.options).forEach((operatorOption) => {
      const shouldHide = this.shouldHideOperator(
        selectedScalarType,
        selectedScalarValue,
        operatorOption,
      )

      if (shouldHide) {
        operatorOption.selected = false
      }
      operatorOption.hidden = shouldHide
    })

    // If the currently selected operator is now hidden, reset the selection.
    if (
      operatorDropdown.selectedOptions.length === 0 ||
      operatorDropdown.selectedOptions[0].hidden
    ) {
      const newSelectedOption = Array.from(operatorDropdown.options).find(
        (option) => !option.hidden,
      )
      if (newSelectedOption) {
        newSelectedOption.selected = true
      }
    }
  }

  /**
   * Determines if an operator should be hidden.
   *   @param {string} selectedScalarType The type of the selected option.
   *   @param {string} selectedScalarValue The value of the selected option.
   *   @param {HTMLOptionElement} operatorOption The operator to check if we should hide.
   * @return {boolean} If the operator should be hidden.
   */
  private static shouldHideOperator(
    selectedScalarType: string | null | undefined,
    selectedScalarValue: string | null,
    operatorOption: HTMLOptionElement,
  ): boolean {
    if (selectedScalarType == null || selectedScalarValue == null) {
      return true
    }

    const operatorScalarMap = window.app?.data?.predicate?.operator_scalars
    if (!operatorScalarMap) {
      return true
    }

    // Get the list of scalars that this operator can be used with.
    const operationScalars: string[] = operatorScalarMap[operatorOption.value]
    if (!operationScalars) {
      return true
    }

    // If this operator is not for the currently selected type, hide it.
    return (
      // Special case for SELECTION scalars (which are of type STRING):
      // do not include EQUAL_TO or NOT_EQUAL_TO. This is because we use a set of checkbox
      // inputs for values for multi-option question predicates, which works well for list
      // operators such as ANY_OF and NONE_OF. Because you can achieve the same functionality
      // of EQUAL_TO with ANY_OF and NOT_EQUAL_TO with NONE_OF, we made a technical choice to
      // exclude these operators from single-select predicates to simplify the code on both
      // the form processing side and on the admin user side.
      !operationScalars.includes(selectedScalarType) ||
      (selectedScalarValue === 'SELECTION' &&
        (operatorOption.value === 'EQUAL_TO' ||
          operatorOption.value === 'NOT_EQUAL_TO'))
    )
  }

  /**
   * Set the input type for question value inputs based on the selected operator, between default and CSV.
   * For CSV operators (IN, NOT_IN), we use a text input to allow comma-separated values.
   * For all other operators, we use a number input.
   *    @param {string} selectedOperatorValue: The currently selected operator.
   *    @param {HTMLElement} defaultInput: The value input element to set the type for.
   *    @param {HTMLElement} csvInputContainer: The text-format input for CSV values.
   */
  private static filterCsvQuestionVisibleInputs(
    selectedOperatorValue: string,
    defaultInput: HTMLElement,
    csvInputContainer: HTMLElement,
  ) {
    let hiddenElements = []
    let shownElements = []
    if (AdminPredicateEdit.CSV_OPERATORS.includes(selectedOperatorValue)) {
      hiddenElements = [defaultInput]
      shownElements = [csvInputContainer]
    } else {
      hiddenElements = [csvInputContainer]
      shownElements = [defaultInput]
    }

    this.disableAndHideAll(hiddenElements)
    this.enableAndShowAll(shownElements)
  }

  /**
   * Set the visible input field for date questions, depending on the selected operator.
   * For age-based operators, we use a number input. For all other operators, we use a date input.
   *    @param {string} selectedOperatorValue: The currently selected operator.
   *    @param {HTMLElement} dateInputContainer: The default (date-format) value input.
   *    @param {HTMLElement} ageInputContainer: The default (age-format) value input.
   *    @param {HTMLElement} secondDateInputContainer: The second (date-format) value input - only used for BETWEEN.
   *    @param {HTMLElement} secondAgeInputContainer: The second (age-format) value input - only used for BETWEEN.
   *    @param {HTMLElement} csvInputContainer: The text-format input for CSV values.
   */
  private static filterDateQuestionVisibleInputs(
    selectedOperatorValue: string,
    dateInputContainer: HTMLElement,
    ageInputContainer: HTMLElement,
    secondDateInputContainer: HTMLElement,
    secondAgeInputContainer: HTMLElement,
    csvInputContainer: HTMLElement,
  ) {
    const ageOperators = ['AGE_BETWEEN', 'AGE_OLDER_THAN', 'AGE_YOUNGER_THAN']

    let hiddenElements: HTMLElement[] = []
    let shownElements: HTMLElement[] = []
    if (ageOperators.includes(selectedOperatorValue)) {
      hiddenElements = [
        dateInputContainer,
        secondDateInputContainer,
        csvInputContainer,
      ]
      shownElements =
        selectedOperatorValue === 'AGE_BETWEEN'
          ? [ageInputContainer, secondAgeInputContainer]
          : [ageInputContainer]
    } else if (
      AdminPredicateEdit.CSV_OPERATORS.includes(selectedOperatorValue)
    ) {
      hiddenElements = [dateInputContainer, ageInputContainer]
      shownElements = [csvInputContainer]
    } else {
      hiddenElements = [
        ageInputContainer,
        secondAgeInputContainer,
        csvInputContainer,
      ]
      shownElements =
        selectedOperatorValue === 'BETWEEN'
          ? [dateInputContainer, secondDateInputContainer]
          : [dateInputContainer]
    }

    this.disableAndHideAll(hiddenElements)
    this.enableAndShowAll(shownElements)
  }

  private static showOrHideDeleteAllConditionsButton() {
    const deleteAllConditionsContainer = document.getElementById(
      'delete-all-conditions-button',
    )
    if (!deleteAllConditionsContainer) {
      return
    }

    // If there's a condition present, show the delete all conditions button.
    // Otherwise, hide and disable.
    const firstConditionElement = document.querySelector('#condition-1')
    if (firstConditionElement) {
      deleteAllConditionsContainer.classList.add('display-flex')
      deleteAllConditionsContainer.hidden = false
    } else {
      deleteAllConditionsContainer.classList.remove('display-flex')
      deleteAllConditionsContainer.hidden = true
    }
  }

  /**
   * Hide the given HTMLElements from display and set disabled to true.
   * Also hides and all child elements, if present.
   */
  private static disableAndHideAll(elements: HTMLElement[]) {
    for (const element of elements) {
      element.setAttribute('disabled', 'disabled')
      element.hidden = true

      const children: HTMLElement[] = Array.from(element.querySelectorAll('*'))
      for (const child of children) {
        child.setAttribute('disabled', 'disabled')
        child.hidden = true
      }
    }
  }

  /**
   * Show and enable the given HTMLElements for display
   * Also shows and enables all child elements, if present.
   */
  private static enableAndShowAll(elements: HTMLElement[]) {
    for (const element of elements) {
      element.removeAttribute('disabled')
      element.hidden = false

      const children: HTMLElement[] = Array.from(element.querySelectorAll('*'))
      for (const child of children) {
        child.removeAttribute('disabled')
        child.hidden = false
      }
    }
  }

  /**
   * Gets the current state of the predicate form to track changes.
   *
   * @returns A serialized representation of the form state.
   */
  private static getPredicateFormState(): string | undefined | null {
    const predicateForm = document.getElementById('predicate-form') as
      | HTMLFormElement
      | undefined
      | null
    if (!predicateForm) {
      return null
    }
    const formData = new FormData(predicateForm)
    const params = new URLSearchParams()
    formData.forEach((value, key) => {
      params.append(key, JSON.stringify(value))
    })

    return params.toString()
  }
}

export function init() {
  new AdminPredicateEdit().onPageLoad()
}
