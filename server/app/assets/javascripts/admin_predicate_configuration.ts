import {addEventListenerToElements} from './dom_utils'

class AdminPredicatConfiguration {
  registerEventListeners() {
    addEventListenerToElements(
      '.cf-scalar-select',
      'input',
      this.configurePredicateFormOnScalarChange.bind(this),
    )

    addEventListenerToElements(
      '.cf-operator-select',
      'input',
      this.configurePredicateFormOnOperatorChange.bind(this),
    )

    // Trigger a select event to set the correct input type on the value field(s)
    Array.from(document.querySelectorAll('.cf-operator-select select')).forEach(
      (el) => {
        const event = new CustomEvent('input', {bubbles: true})
        el.dispatchEvent(event)
      },
    )

    document
      .querySelector('#predicate-add-value-set')
      ?.addEventListener('click', this.predicateAddValueRow.bind(this))

    addEventListenerToElements(
      'predicate-config-delete-value-row',
      'click',
      this.predicateDeleteValueRow.bind(this),
    )
  }

  configurePredicateFormOnScalarChange(event: Event) {
    // Get the type of scalar currently selected.
    const scalarDropdown = event.target as HTMLSelectElement
    const selectedScalarType =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type
    const selectedScalarValue =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].value

    filterOperators(scalarDropdown, selectedScalarType, selectedScalarValue)
  }

  configurePredicateFormOnOperatorChange(event: Event) {
    const operatorDropdown = event.target as HTMLSelectElement
    const selectedOperatorValue =
      operatorDropdown.options[operatorDropdown.options.selectedIndex].value
    const questionId = (
      operatorDropdown.closest('.cf-operator-select') as HTMLElement
    )?.dataset?.questionId

    const commaSeparatedHelpTexts = document.querySelectorAll(
      `#predicate-config-value-row-container [data-question-id="${questionId}"] .cf-predicate-value-comma-help-text`,
    )

    // This help text div isn't present at all in some cases.
    if (commaSeparatedHelpTexts == null) {
      return
    }

    const shouldShowCommaSeperatedHelpText =
      selectedOperatorValue.toUpperCase() !== 'IN' &&
      selectedOperatorValue.toUpperCase() !== 'NOT_IN'

    for (const commaSeparatedHelpText of Array.from(commaSeparatedHelpTexts)) {
      commaSeparatedHelpText.classList.toggle(
        'hidden',
        shouldShowCommaSeperatedHelpText,
      )
    }

    // The type of the value field may need to change based on the current operator
    const scalarDropdown = this.getElementWithQuestionId(
      '.cf-scalar-select',
      questionId,
      'select',
    ) as HTMLSelectElement
    const selectedScalarType =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type
    const selectedScalarValue =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].value
    this.configurePredicateValueInput(
      scalarDropdown,
      selectedScalarType,
      selectedScalarValue,
      questionId,
    )
  }

  /**
   *  Setup the the html attributes for value inputs so they acccept the correct
   *  type of input (nubers, text, email, ect.)
   *  @param {HTMLSelectElement} scalarDropdown The element to configure the value input for.
   *  @param {string} selectedScalarType The tyoe of the selected option
   *  @param {string} selectedScalarValue The value of the selected option
   */
  configurePredicateValueInput(
    scalarDropdown: HTMLSelectElement,
    selectedScalarType: string,
    selectedScalarValue: string,
    questionId: string,
  ) {
    // If the scalar is from a multi-option question, there is not an input box for the 'Value'
    // field (there's a set of checkboxes instead), so return immediately.
    if (
      selectedScalarValue.toUpperCase() === 'SELECTION' ||
      selectedScalarValue.toUpperCase() === 'SELECTIONS'
    ) {
      return
    }

    const operatorDropdown = this.getElementWithQuestionId(
      '.cf-operator-select',
      questionId,
      'select',
    ) as HTMLSelectElement
    const operatorValue =
      operatorDropdown.options[operatorDropdown.options.selectedIndex].value

    const valueInputs = document
      .querySelector('#predicate-config-value-row-container')
      .querySelectorAll(`[data-question-id="${questionId}"] input`)

    for (const valueInput of Array.from(valueInputs)) {
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
        case 'CURRENCY_CENTS':
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
  }

  predicateAddValueRow(event: Event) {
    event.preventDefault()
    event.stopPropagation()

    const currentRows = document.querySelectorAll('.predicate-config-value-row')
    const templateRow = currentRows[currentRows.length - 1]
    const newRow = templateRow.cloneNode(true) as HTMLElement

    newRow.querySelectorAll('input').forEach((el) => {
      if (el.type === 'checkbox' || el.type === 'radio') {
        el.checked = false
      } else {
        el.value = ''
      }

      let groupNum = parseInt(el.name.match(/group-(\d+)/)[1], 10)
      el.name = el.name.replace(/group-\d+/, `group-${++groupNum}`)
      const newId = `${el.id}-${groupNum}`
      el.id = newId

      el.closest('label')?.setAttribute('for', newId)
    })

    const deleteButtonDiv = newRow.querySelector(
      '.predicate-config-delete-value-row',
    )

    deleteButtonDiv.addEventListener(
      'click',
      this.predicateDeleteValueRow.bind(this),
    )
    deleteButtonDiv.querySelector('svg').classList.remove('hidden')

    document
      .getElementById('predicate-config-value-row-container')
      .append(newRow)
  }

  predicateDeleteValueRow(event: Event) {
    event.preventDefault()
    event.stopPropagation()

    const valueRow = (event.target as HTMLElement).closest(
      '.predicate-config-value-row',
    )
    valueRow.remove()
  }

  /**
   * Filter the operators available for each scalar type based on the current scalar selected.
   *   @param {HTMLSelectElement} scalarDropdown The element to filter the operators for.
   *   @param {string} selectedScalarType The tyoe of the selected option
   *   @param {string} selectedScalarValue The value of the selected option
   */
  filterOperators(
    scalarDropdown: HTMLSelectElement,
    selectedScalarType: string,
    selectedScalarValue: string,
  ) {
    const questionId = this.getQuestionId(scalarDropdown)
    const operatorDropdown = this.getElementWithQuestionId(
      '.cf-operator-select',
      questionId,
      'select',
    ) as HTMLSelectElement

    Array.from(operatorDropdown.options).forEach((operatorOption) => {
      operatorOption.classList.toggle(
        'hidden',
        !this.shouldHideOperator(
          selectedScalarType,
          selectedScalarValue,
          operatorOption,
        ),
      )
    })
  }

  /**
   * Logic that decides if a operator should be hidden.
   *   @param {string} selectedScalarType The tyoe of the selected option
   *   @param {string} selectedScalarValue The value of the selected option
   *   @param {HTMLOptionElement} operatorOption The operator to check if we should hide.
   * @return {boolean} If the operator should be hidden
   */
  private shouldHideOperator(
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

  private getQuestionId(element: HTMLSelectElement) {
    return (element.closest(`[data-question-id]`) as HTMLElement)?.dataset
      .questionId
  }

  private getElementWithQuestionId(
    selector: string,
    questionId: string,
    childSelector: string,
  ) {
    return document.querySelector(
      `${selector}[data-question-id="${questionId}"] ${childSelector}`,
    )
  }
}

function configurePredicateFormOnScalarChange(event: Event) {
  // Get the type of scalar currently selected.
  const scalarDropdown = event.target as HTMLSelectElement
  const selectedScalarType =
    scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type
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
  const operatorDropdown = scalarDropdown
    .closest('.cf-predicate-options') // div containing all predicate builder form fields
    .querySelector('.cf-operator-select') // div containing the operator dropdown
    .querySelector('select') as HTMLSelectElement

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

  const operatorDropdown = scalarDropdown
    .closest('.cf-predicate-options') // div containing all predicate builder form fields
    .querySelector('.cf-operator-select') // div containing the operator dropdown
    .querySelector('select')
  const operatorValue =
    operatorDropdown.options[operatorDropdown.options.selectedIndex].value

  const valueInput = scalarDropdown
    .closest('.cf-predicate-options') // div containing all predicate builder form fields
    .querySelector('.cf-predicate-value-input') // div containing the predicate value input
    .querySelector('input')

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
    .querySelector('.cf-predicate-value-comma-help-text')

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
  const scalarDropdown = operatorDropdown
    .closest('.cf-predicate-options') // div containing all predicate builder form fields
    .querySelector('.cf-scalar-select') // div containing the scalar dropdown
    .querySelector('select')
  const selectedScalarType =
    scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type
  const selectedScalarValue =
    scalarDropdown.options[scalarDropdown.options.selectedIndex].value
  configurePredicateValueInput(
    scalarDropdown,
    selectedScalarType,
    selectedScalarValue,
  )
}

export function init() {
  // Hacky feature flag for the V2 predicate configuration UI.
  if (document.querySelector('.predicate-config-value-row') == null) {
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
  } else {
    new AdminPredicatConfiguration().registerEventListeners()
  }
}
