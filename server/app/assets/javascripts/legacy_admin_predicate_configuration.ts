/** This this file contains functionality for the legacy predicate editor in the
 * admin UI. */
/** TODO(4004): Remove this legacy file once new predicate UI is fully released. */

import {addEventListenerToElements, assertNotNull} from './util'

function configurePredicateFormOnScalarChange(event: Event) {
  // Get the type of scalar currently selected.
  const scalarDropdown = event.target as HTMLSelectElement
  const selectedScalarType = assertNotNull(
    scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type,
  )
  const selectedScalarValue =
    scalarDropdown.options[scalarDropdown.options.selectedIndex].value

  filterOperators(scalarDropdown, selectedScalarType, selectedScalarValue)
  configurePredicateValueInput(
    scalarDropdown,
    selectedScalarType,
    selectedScalarValue,
  )
}

/**
 * Filter the operators available for each scalar type based on the current scalar selected.
 *   @param {HTMLSelectElement} scalarDropdown The element to filter the operators for.
 *   @param {string} selectedScalarType The tyoe of the selected option
 *   @param {string} selectedScalarValue The value of the selected option
 */
function filterOperators(
  scalarDropdown: HTMLSelectElement,
  selectedScalarType: string,
  selectedScalarValue: string,
) {
  // Filter the operators available for the given selected scalar type.
  const operatorDropdown = assertNotNull(
    scalarDropdown
      .closest('.cf-predicate-options') // div containing all predicate builder form fields
      ?.querySelector<HTMLSelectElement>('.cf-operator-select select'),
  )

  Array.from(operatorDropdown.options).forEach((operatorOption) => {
    // Remove any existing hidden class from previous filtering.
    operatorOption.classList.remove('hidden')

    if (
      shouldHideOperator(
        selectedScalarType,
        selectedScalarValue,
        operatorOption,
      )
    ) {
      operatorOption.classList.add('hidden')
    }
  })
}

/**
 * Logic that decides if a operator should be hidden.
 *   @param {string} selectedScalarType The tyoe of the selected option
 *   @param {string} selectedScalarValue The value of the selected option
 *   @param {HTMLOptionElement} operatorOption The operator to check if we should hide.
 * @return {boolean} If the operator should be hidden
 */
function shouldHideOperator(
  selectedScalarType: string,
  selectedScalarValue: string,
  operatorOption: HTMLOptionElement,
): boolean {
  // If this operator is not for the currently selected type, hide it.
  return (
    // Special case for SELECTION scalars (which are of type STRING):
    // do not include EQUAL_TO or NOT_EQUAL_TO. This is because we use a set of checkbox
    // inputs for values for multi-option question predicates, which works well for list
    // operators such as ANY_OF and NONE_OF. Because you can achieve the same functionality
    // of EQUAL_TO with ANY_OF and NOT_EQUAL_TO with NONE_OF, we made a technical choice to
    // exclude these operators from single-select predicates to simplify the code on both
    // the form processing side and on the admin user side.
    !(selectedScalarType in operatorOption.dataset) ||
    (selectedScalarValue.toUpperCase() === 'SELECTION' &&
      (operatorOption.value === 'EQUAL_TO' ||
        operatorOption.value === 'NOT_EQUAL_TO'))
  )
}

/**
 *  Setup the the html attributes for value inputs so they acccept the correct
 *  type of input (nubers, text, email, ect.)
 *  @param {HTMLSelectElement} scalarDropdown The element to configure the value input for.
 *  @param {string} selectedScalarType The tyoe of the selected option
 *  @param {string} selectedScalarValue The value of the selected option
 */
function configurePredicateValueInput(
  scalarDropdown: HTMLSelectElement,
  selectedScalarType: string,
  selectedScalarValue: string,
) {
  // If the scalar is from a multi-option question, there is not an input box for the 'Value'
  // field (there's a set of checkboxes instead), so return immediately.
  if (
    selectedScalarValue.toUpperCase() === 'SELECTION' ||
    selectedScalarValue.toUpperCase() === 'SELECTIONS'
  ) {
    return
  }

  const operatorDropdown = assertNotNull(
    scalarDropdown
      .closest('.cf-predicate-options') // div containing all predicate builder form fields
      ?.querySelector<HTMLSelectElement>('.cf-operator-select select'),
  )
  const operatorValue =
    operatorDropdown.options[operatorDropdown.options.selectedIndex].value

  const valueInput = assertNotNull(
    scalarDropdown
      .closest('.cf-predicate-options') // div containing all predicate builder form fields
      ?.querySelector<HTMLInputElement>('.cf-predicate-value-input input'),
  )

  switch (selectedScalarType.toUpperCase()) {
    case 'STRING':
      if (selectedScalarValue.toUpperCase() === 'EMAIL') {
        // Need to look at the selected scalar *value* for email since the type is just a
        // string, but emails have a special type in HTML inputs.
        valueInput.setAttribute('type', 'email')
        break
      }
      valueInput.setAttribute('type', 'text')
      break
    case 'LONG':
      if (
        operatorValue.toUpperCase() === 'IN' ||
        operatorValue.toUpperCase() === 'NOT_IN'
      ) {
        // IN and NOT_IN operate on lists of longs, which must be entered as a comma-separated list
        valueInput.setAttribute('type', 'text')
      } else {
        valueInput.setAttribute('type', 'number')
      }
      break
    case 'DATE':
      valueInput.setAttribute('type', 'date')
      break
    default:
      valueInput.setAttribute('type', 'text')
  }
}

function configurePredicateFormOnOperatorChange(event: Event) {
  const operatorDropdown = event.target as HTMLSelectElement
  const selectedOperatorValue =
    operatorDropdown.options[operatorDropdown.options.selectedIndex].value

  const commaSeparatedHelpText = operatorDropdown
    .closest('.cf-predicate-options')
    ?.querySelector('.cf-predicate-value-comma-help-text')

  // This help text div isn't present at all in some cases.
  if (!commaSeparatedHelpText) {
    return
  }

  // Remove any existing hidden class.
  commaSeparatedHelpText.classList.remove('hidden')

  if (
    selectedOperatorValue.toUpperCase() !== 'IN' &&
    selectedOperatorValue.toUpperCase() !== 'NOT_IN'
  ) {
    commaSeparatedHelpText.classList.add('hidden')
  }

  // The type of the value field may need to change based on the current operator
  const scalarDropdown = assertNotNull(
    operatorDropdown
      .closest('.cf-predicate-options') // div containing all predicate builder form fields
      ?.querySelector<HTMLSelectElement>('.cf-scalar-select select'),
  )
  const selectedScalarType = assertNotNull(
    scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type,
  )
  const selectedScalarValue =
    scalarDropdown.options[scalarDropdown.options.selectedIndex].value
  configurePredicateValueInput(
    scalarDropdown,
    selectedScalarType,
    selectedScalarValue,
  )
}

export function init() {
  if (document.querySelector('.predicate-config-value-row') != null) {
    return
  }

  addEventListenerToElements(
    '.cf-operator-select',
    'input',
    configurePredicateFormOnOperatorChange,
  )

  // Configure the admin predicate builder to show the appropriate options based on
  // the type of scalar selected.
  addEventListenerToElements(
    '.cf-scalar-select',
    'input',
    configurePredicateFormOnScalarChange,
  )
}
