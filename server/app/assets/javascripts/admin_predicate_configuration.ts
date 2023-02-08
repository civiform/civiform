import {addEventListenerToElements, assertNotNull} from './util'

/** Dynamic behavior for ProgramBlockPredicateConfigureView.
 *
 * To configure a predicate, the admin specifies the scalar of a question
 * the predicate refers to, a comparison operator, and a comparison value
 * or values.
 *
 * The UI supports specifying groups of comparison values that taken together
 * form an AND clause in the resulting boolean expression AST. These groups
 * of comparison values all share a group number. The groups are added
 * and deleted as rows of value inputs in the UI.
 *
 * When scalar is selected, this code updates the operator input and value
 * input(s) to provide the appropriate input semantics.
 *
 * When operator is selected, only value input(s) are updated.
 * */
class AdminPredicateConfiguration {
  registerEventListeners() {
    if (document.querySelector('.predicate-config-value-row') == null) {
      return
    }

    addEventListenerToElements('.cf-scalar-select', 'input', (event: Event) =>
      this.configurePredicateFormOnScalarChange(event),
    )

    addEventListenerToElements('.cf-operator-select', 'input', (event: Event) =>
      this.configurePredicateFormOnOperatorChange(event),
    )

    // Trigger a select event to set the correct input type on the value field(s)
    Array.from(document.querySelectorAll('.cf-scalar-select select')).forEach(
      (el) => {
        const event = new CustomEvent('input', {bubbles: true})
        el.dispatchEvent(event)
      },
    )

    // Set add and remove events for managing rows of values when multiple questions are involved.
    document
      .querySelector('#predicate-add-value-row')
      ?.addEventListener('click', (event: Event) =>
        this.predicateAddValueRow(event),
      )

    addEventListenerToElements(
      '.predicate-config-delete-value-row',
      'click',
      (event: Event) => this.predicateDeleteValueRow(event),
    )
  }

  configurePredicateFormOnScalarChange(event: Event) {
    // Get the type of scalar currently selected.
    const scalarDropdown = event.target as HTMLSelectElement
    const questionId = assertNotNull(this.getQuestionId(scalarDropdown))
    const selectedScalarType =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type!
    const selectedScalarValue =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].value

    this.filterOperators(
      scalarDropdown,
      selectedScalarType,
      selectedScalarValue,
    )
    this.configurePredicateValueInput(
      selectedScalarType,
      selectedScalarValue,
      questionId,
    )
  }

  /** Updates the value input and hidden behavior of CSV help text when the operator changes.
   *
   * The CSV help text instructs the admin on using a single text field to specify multiple
   * values in cases where they select an operator that acts on multiple values such as ANY_OF
   * or NONE_OF.
   * */
  configurePredicateFormOnOperatorChange(event: Event) {
    const operatorDropdown = event.target as HTMLSelectElement
    const selectedOperatorValue =
      operatorDropdown.options[operatorDropdown.options.selectedIndex].value
    const operatorDropdownContainer = assertNotNull(
      operatorDropdown.closest('.cf-operator-select'),
    ) as HTMLElement

    const questionId = assertNotNull(
      operatorDropdownContainer.dataset.questionId,
    )

    const csvHelpTexts = document.querySelectorAll(
      `#predicate-config-value-row-container [data-question-id="${questionId}"] .cf-predicate-value-comma-help-text`,
    )

    // The help text div is present for inputs that allow specifying
    // multiple values in a single text input. It won't be present
    // for other input types e.g. multiselect inputs.
    if (csvHelpTexts != null) {
      const shouldShowCommaSeperatedHelpText =
        selectedOperatorValue.toUpperCase() !== 'IN' &&
        selectedOperatorValue.toUpperCase() !== 'NOT_IN'

      for (const commaSeparatedHelpText of Array.from(csvHelpTexts)) {
        commaSeparatedHelpText.classList.toggle(
          'hidden',
          shouldShowCommaSeperatedHelpText,
        )
      }
    }

    // Update the value field to reflect the new Operator selection.
    const scalarDropdown = this.getElementWithQuestionId(
      '.cf-scalar-select',
      questionId,
      'select',
    ) as HTMLSelectElement
    const selectedScalarType = assertNotNull(
      scalarDropdown.options[scalarDropdown.options.selectedIndex].dataset.type,
    )
    const selectedScalarValue =
      scalarDropdown.options[scalarDropdown.options.selectedIndex].value
    this.configurePredicateValueInput(
      selectedScalarType,
      selectedScalarValue,
      questionId,
    )
  }

  /**
   *  Setup the the html attributes for value inputs so they acccept the correct
   *  type of input (nubers, text, email, etc.)
   *  @param {string} selectedScalarType The type of the selected option.
   *  @param {string} selectedScalarValue The value of the selected option.
   *  @param {number} questionId The ID of the question for this predicate value.
   */
  configurePredicateValueInput(
    selectedScalarType: string | null,
    selectedScalarValue: string | null,
    questionId: string,
  ) {
    // If the scalar is from a multi-option or address question, there is not an input box
    // for the 'Value' field (there's a set of checkboxes instead), so return immediately.
    if (
      selectedScalarValue &&
      (selectedScalarValue.toUpperCase() === 'SELECTION' ||
        selectedScalarValue.toUpperCase() === 'SELECTIONS' ||
        selectedScalarValue.toUpperCase() === 'SERVICE_AREA')
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

    const valueInputs = assertNotNull(
      document
        ?.querySelector('#predicate-config-value-row-container')
        ?.querySelectorAll(`[data-question-id="${questionId}"] input`),
    )

    for (const valueInput of Array.from(valueInputs)) {
      // Reset defaults
      valueInput.setAttribute('type', 'text')
      valueInput.removeAttribute('step')
      valueInput.removeAttribute('placeholder')

      if (selectedScalarType == null || selectedScalarValue == null) {
        continue
      }

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
          if (
            operatorValue.toUpperCase() === 'IN' ||
            operatorValue.toUpperCase() === 'NOT_IN'
          ) {
            // IN and NOT_IN operate on lists of longs, which must be entered as a comma-separated list
            valueInput.setAttribute('type', 'text')
          } else {
            valueInput.setAttribute('step', '.01')
            valueInput.setAttribute('placeholder', '$0.00')
            valueInput.setAttribute('type', 'number')
          }
          break
        case 'LONG':
          if (
            operatorValue.toUpperCase() === 'IN' ||
            operatorValue.toUpperCase() === 'NOT_IN'
          ) {
            // IN and NOT_IN operate on lists of longs, which must be entered as a comma-separated list
            valueInput.setAttribute('type', 'text')
          } else {
            valueInput.setAttribute('step', '1')
            valueInput.setAttribute('placeholder', '0')
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

  /** Add a value row by duplicating the last row in the list and updating its group IDs. */
  predicateAddValueRow(event: Event) {
    event.preventDefault()
    event.stopPropagation()

    const currentRows = document.querySelectorAll('.predicate-config-value-row')
    const templateRow = currentRows[currentRows.length - 1]
    const newRow = templateRow.cloneNode(/* deep= */ true) as HTMLElement

    newRow.querySelectorAll('input').forEach((el: HTMLInputElement) => {
      if (el.type === 'checkbox' || el.type === 'radio') {
        el.checked = false
      } else {
        el.value = ''
      }

      const groupNumString = assertNotNull(el.name.match(/group-(\d+)/))[1]

      let groupNum = parseInt(groupNumString, 10)
      el.name = el.name.replace(/group-\d+/, `group-${++groupNum}`)

      // The server-rendered inputs have UUID-generated IDs to ensure uniqueness on the page.
      // We reuse those IDs and add a suffix to likewise ensure uniqueness for the new inputs.
      const newId = `${el.id}-${groupNum}`
      el.id = newId

      el.closest('label')?.setAttribute('for', newId)
    })

    const deleteButtonDiv = assertNotNull(
      newRow.querySelector('.predicate-config-delete-value-row'),
    ) as HTMLElement

    deleteButtonDiv.addEventListener('click', (event: Event) =>
      this.predicateDeleteValueRow(event),
    )
    // The first row which is being used as a template cannot be deleted
    // and is rendered with a hidden delete button.
    assertNotNull(deleteButtonDiv.querySelector('svg')).classList.remove(
      'hidden',
    )

    assertNotNull(
      document.getElementById('predicate-config-value-row-container'),
    ).append(newRow)
  }

  /**
   * Delete the value row that includes the event target.
   * */
  predicateDeleteValueRow(event: Event) {
    event.preventDefault()
    event.stopPropagation()

    const valueRow = (event.target as HTMLElement).closest(
      '.predicate-config-value-row',
    )

    if (valueRow == null) {
      throw new Error('Parent with class .predicate-config-value-row not found')
    }

    valueRow.remove()
  }

  /**
   * Filter the operators available for each scalar type based on the current scalar selected.
   *   @param {HTMLSelectElement} scalarDropdown The element to filter the operators for.
   *   @param {string} selectedScalarType The tyoe of the selected option.
   *   @param {string} selectedScalarValue The value of the selected option.
   */
  filterOperators(
    scalarDropdown: HTMLSelectElement,
    selectedScalarType: string | null,
    selectedScalarValue: string | null,
  ) {
    const questionId = assertNotNull(this.getQuestionId(scalarDropdown))
    const operatorDropdown = this.getElementWithQuestionId(
      '.cf-operator-select',
      questionId,
      'select',
    ) as HTMLSelectElement

    Array.from(operatorDropdown.options).forEach((operatorOption) => {
      const shouldHide = this.shouldHideOperator(
        selectedScalarType,
        selectedScalarValue,
        operatorOption,
      )

      if (shouldHide) {
        operatorOption.selected = false
      }

      operatorOption.classList.toggle('hidden', /* force= */ shouldHide)
    })
  }

  /**
   * Determines if an operator should be hidden.
   *   @param {string} selectedScalarType The tyoe of the selected option.
   *   @param {string} selectedScalarValue The value of the selected option.
   *   @param {HTMLOptionElement} operatorOption The operator to check if we should hide.
   * @return {boolean} If the operator should be hidden.
   */
  private shouldHideOperator(
    selectedScalarType: string | null,
    selectedScalarValue: string | null,
    operatorOption: HTMLOptionElement,
  ): boolean {
    if (selectedScalarType == null || selectedScalarValue == null) {
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
      !(selectedScalarType in operatorOption.dataset) ||
      (selectedScalarValue.toUpperCase() === 'SELECTION' &&
        (operatorOption.value === 'EQUAL_TO' ||
          operatorOption.value === 'NOT_EQUAL_TO'))
    )
  }

  private getQuestionId(element: HTMLSelectElement): string | undefined {
    const parentWithQuestionId = assertNotNull(
      element.closest(`[data-question-id]`),
    ) as HTMLElement
    return parentWithQuestionId.dataset.questionId
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

export function init() {
  new AdminPredicateConfiguration().registerEventListeners()
}
