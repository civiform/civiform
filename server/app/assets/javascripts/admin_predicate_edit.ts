import {HtmxAfterSwapEvent} from './htmx_request'
import {addEventListenerToElements} from './util'

export class AdminPredicateEdit {
  // Set in server/app/views/admin/programs/predicates/PredicateValuesInputFragment.html
  static VALUE_INPUT_ID_SUFFIX: string = '-value'
  static VALUE_INPUT_HINT_ID_SUFFIX: string = '-valueHintText'
  static SECOND_VALUE_INPUT_ID_SUFFIX: string = '-secondValue'
  static SECOND_VALUE_INPUT_GROUP_ID_SUFFIX: string = '-secondValueGroup'

  static onHtmxAfterSwap(event: HtmxAfterSwapEvent): void {
    // Only update if the target is the 'subcondition-container' element in this swap
    if (event.target.classList.contains('subcondition-container')) {
      // Remove existing listeners and bind to new ones after the swap
      // replaces the html to ensure there's only one per element instead of
      // appending after each swap.
      document
        .querySelectorAll<HTMLSelectElement>('.cf-predicate-scalar-select')
        .forEach((dropdown: HTMLSelectElement) => {
          dropdown.removeEventListener('change', this.onScalarDropdownChange)
          dropdown.addEventListener('change', this.onScalarDropdownChange)
        })
      document
        .querySelectorAll<HTMLSelectElement>('.cf-predicate-operator-select')
        .forEach((dropdown: HTMLSelectElement) => {
          dropdown.removeEventListener('change', this.onOperatorDropdownChange)
          dropdown.addEventListener('change', this.onOperatorDropdownChange)
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
  }

  onPageLoad(): void {
    addEventListenerToElements(
      '.cf-predicate-scalar-select',
      'change',
      AdminPredicateEdit.onScalarDropdownChange,
    )
    addEventListenerToElements(
      '.cf-predicate-operator-select',
      'change',
      AdminPredicateEdit.onOperatorDropdownChange,
    )
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
    const valueInputId = valueBaseId + AdminPredicateEdit.VALUE_INPUT_ID_SUFFIX
    const secondValueInputId =
      valueBaseId + AdminPredicateEdit.SECOND_VALUE_INPUT_ID_SUFFIX
    const valueInput = document.getElementById(valueInputId) as HTMLElement
    const secondValueInput = document.getElementById(
      secondValueInputId,
    )

    if (!valueInput) {
      return
    }

    // Number question types
    // Use number inputs for single values, text inputs for CSV
    if (valueInput.hasAttribute('number-value')) {
      this.setNumberQuestionValueInputType(selectedOperatorValue, valueInput)
    }

    // Date question types
    // Use number inputs for age-based operator, date inputs for all others
    if (valueInput.hasAttribute('date-value')) {
      this.setDateQuestionValueInputType(
        selectedOperatorValue,
        valueInput,
        secondValueInput,
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

    // Get the second value input (for enabling/disabling on the form)
    const secondValueInputId =
      valueBaseId + AdminPredicateEdit.SECOND_VALUE_INPUT_ID_SUFFIX
    const secondValueInput = document.getElementById(secondValueInputId)
    if (!secondValueInput) {
      return
    }

    // Show or hide the second value input based on the selected operator.
    // Currently only BETWEEN operators require a second value.
    if (
      selectedOperatorValue === 'BETWEEN' ||
      selectedOperatorValue === 'AGE_BETWEEN'
    ) {
      secondValueInputGroup.hidden = false
      secondValueInput.ariaDisabled = 'false'
    } else {
      secondValueInputGroup.hidden = true
      secondValueInput.ariaDisabled = 'true'
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
    if (selectedOperatorValue === 'IN' || selectedOperatorValue === 'NOT_IN') {
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
   * Set the input type for number question value inputs based on the selected operator.
   * For CSV operators (IN, NOT_IN), we use a text input to allow comma-separated values.
   * For all other operators, we use a number input.
   *    @param {string} selectedOperatorValue: The currently selected operator.
   *    @param {HTMLElement} valueInput: The value input element to set the type for.
   */
  private static setNumberQuestionValueInputType(
    selectedOperatorValue: string,
    valueInput: HTMLElement,
  ) {
    const csvOperators = ['IN', 'NOT_IN']
    if (csvOperators.includes(selectedOperatorValue)) {
      valueInput.setAttribute('type', 'text')
      valueInput.setAttribute('inputmode', 'text')
    } else {
      valueInput.setAttribute('type', 'number')
      valueInput.setAttribute('inputmode', 'decimal')
    }
  }

  /**
   * Set the input type for date question value inputs based on the selected operator.
   * For age-based operators, we use a number input. For all other operators, we use a date input.
   *    @param {string} selectedOperatorValue: The currently selected operator.
   *    @param {HTMLElement} valueInput: The value input element to set the type for.
   *    @param {HTMLElement | null} secondValueInput: The second value input element to set the type for, if it exists.
   */
  private static setDateQuestionValueInputType(
    selectedOperatorValue: string,
    valueInput: HTMLElement,
    secondValueInput: HTMLElement | null,
  ) {
    const ageOperators = ['AGE_BETWEEN', 'AGE_OLDER_THAN', 'AGE_YOUNGER_THAN']
    if (ageOperators.includes(selectedOperatorValue)) {
      valueInput.setAttribute('type', 'number')
      secondValueInput?.setAttribute('type', 'number')
    } else {
      valueInput.setAttribute('type', 'date')
      secondValueInput?.setAttribute('type', 'date')
    }
  }
}

export function init() {
  new AdminPredicateEdit().onPageLoad()
}
