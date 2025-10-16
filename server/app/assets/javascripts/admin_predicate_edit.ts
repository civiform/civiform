import {HtmxAfterSwapEvent} from './htmx_request'
import {addEventListenerToElements} from './util'

export class AdminPredicateEdit {
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

      // Trigger change to update operators based on the current scalar selected.
      Array.from(
        document.querySelectorAll('.cf-predicate-scalar-select select'),
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
}

export function init() {
  new AdminPredicateEdit().onPageLoad()
}
