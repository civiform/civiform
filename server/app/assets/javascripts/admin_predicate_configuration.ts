import {ToastController} from './toast'
import {addEventListenerToElements, assertNotNull} from './util'

/**
 * Dynamic behavior for ProgramBlockPredicateConfigureView.
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
 */
class AdminPredicateConfiguration {
  registerEventListeners() {
    addEventListenerToElements(
      '#add-replace-predicate-condition',
      'click',
      (event: Event) => this.predicateAddOrReplaceCondition(event),
    )

    // The rest of the event listeners are specific to the predicate configuration page.
    if (document.querySelector('.predicate-config-value-row') == null) {
      return
    }

    addEventListenerToElements(
      '#predicate-submit-button',
      'click',
      (event: Event) => this.validateSubmit(event),
    )

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

  validateSubmit(event: Event) {
    let hasSelectionMissing = false
    let hasValueMissing = false

    // Check if any scalars are missing a value.
    document
      .querySelectorAll<HTMLSelectElement>('.cf-scalar-select select')
      .forEach((el: HTMLSelectElement) => {
        if (el.options[el.options.selectedIndex].value == '') {
          hasSelectionMissing = true
        }
      })

    // Check if any operators are missing a value.
    document
      .querySelectorAll<HTMLSelectElement>('.cf-operator-select select')
      .forEach((el: HTMLSelectElement) => {
        if (el.options[el.options.selectedIndex].value == '') {
          hasSelectionMissing = true
        }
      })

    // Check if any inputs are missing a value.
    document
      .querySelector('#predicate-config-value-row-container')!
      .querySelectorAll('[data-question-id]')
      .forEach((questionAnswerGroup) => {
        let needsCheckedOption = false
        let hasCheckedOption = false
        questionAnswerGroup.querySelectorAll('input').forEach((input) => {
          if (input.type == 'checkbox') {
            needsCheckedOption = true
            if (input.checked) {
              hasCheckedOption = true
            }
          } else if (input.value == '') {
            hasValueMissing = true
          }
        })
        if (needsCheckedOption && !hasCheckedOption) {
          hasValueMissing = true
        }
      })

    // If there are issues with any of the fields, we show a toast and prevent submit.
    let errorMessage
    if (hasSelectionMissing && hasValueMissing) {
      errorMessage =
        'One or more form fields or dropdowns is missing an entry. Please fill out all form fields and dropdowns before saving.'
    } else if (hasSelectionMissing) {
      errorMessage =
        'One or more dropdowns is missing a selection. Please fill out all dropdowns before saving.'
    } else if (hasValueMissing) {
      errorMessage =
        'One or more form fields is missing an entry. Please fill out all form fields before saving.'
    }

    if (errorMessage) {
      event.preventDefault()
      this.showErrorMessage(errorMessage)
    }
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
    this.configurePredicateValueInputs(
      selectedScalarType,
      selectedScalarValue,
      questionId,
    )
  }

  predicateAddOrReplaceCondition(event: Event) {
    const numChecked = Array.from(
      document.querySelectorAll<HTMLInputElement>(
        '.cf-predicate-question-options',
      ),
    ).filter((el) => el.checked).length

    if (numChecked == 0) {
      event.preventDefault()
      this.showErrorMessage('Please select a question.')
    }
  }

  /** Scrolls to the top of the page and shows an error toast.  */
  showErrorMessage(errorMessage: string) {
    // Scroll to the top of the page to ensure the user sees the error message.
    window.scrollTo(0, 0)
    ToastController.showToastMessage({
      id: `predicate-issue-${Math.random()}`,
      content: errorMessage,
      duration: -1,
      type: 'error',
      condOnStorageKey: null,
      canDismiss: true,
      canIgnore: false,
    })
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

    // Each value input has its own help text
    const csvHelpTexts = document.querySelectorAll(
      `#predicate-config-value-row-container [data-question-id="${questionId}"] .cf-predicate-value-comma-help-text`,
    )
    csvHelpTexts.forEach((div: Element) =>
      div.classList.toggle(
        'hidden',
        selectedOperatorValue !== 'IN' && selectedOperatorValue !== 'NOT_IN',
      ),
    )

    // Each value input has its own help text
    const betweenHelpTexts = document.querySelectorAll(
      `#predicate-config-value-row-container [data-question-id="${questionId}"] .cf-predicate-value-between-help-text`,
    )
    betweenHelpTexts.forEach((div: Element) =>
      div.classList.toggle('hidden', selectedOperatorValue !== 'AGE_BETWEEN'),
    )

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
    this.configurePredicateValueInputs(
      selectedScalarType,
      selectedScalarValue,
      questionId,
    )
  }

  /**
   *  Setup the the html attributes for value inputs so they accept the correct
   *  type of input (numbers, text, email, etc.)
   *  @param {string} selectedScalarType The type of the selected option.
   *  @param {string} selectedScalarValue The value of the selected option.
   *  @param {number} questionId The ID of the question for this predicate value.
   */
  configurePredicateValueInputs(
    selectedScalarType: string | null,
    selectedScalarValue: string | null,
    questionId: string,
  ) {
    // If the scalar is from a multi-option or address question, there is not an input box
    // for the 'Value' field (there's a set of checkboxes instead), so return immediately.
    if (
      selectedScalarValue &&
      (selectedScalarValue === 'SELECTION' ||
        selectedScalarValue === 'SELECTIONS' ||
        selectedScalarValue === 'SERVICE_AREA')
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

    document
      .querySelector('#predicate-config-value-row-container')!
      .querySelectorAll(`[data-question-id="${questionId}"] input`)
      .forEach((valueInput) =>
        this.setInputAttributes(
          valueInput,
          selectedScalarType,
          selectedScalarValue,
          operatorValue,
        ),
      )
  }

  setInputAttributes(
    valueInput: Element,
    selectedScalarType: string | null,
    selectedScalarValue: string | null,
    operatorValue: string,
  ) {
    // Reset defaults
    valueInput.setAttribute('type', 'text')
    valueInput.removeAttribute('step')
    valueInput.removeAttribute('placeholder')

    if (selectedScalarType == null || selectedScalarValue == null) {
      return
    }

    switch (selectedScalarType) {
      case 'STRING':
        if (selectedScalarValue === 'EMAIL') {
          // Need to look at the selected scalar *value* for email since the type is just a
          // string, but emails have a special type in HTML inputs.
          valueInput.setAttribute('type', 'email')
          break
        }
        valueInput.setAttribute('type', 'text')
        break
      case 'CURRENCY_CENTS':
        if (operatorValue === 'IN' || operatorValue === 'NOT_IN') {
          // IN and NOT_IN operate on lists of longs, which must be entered as a comma-separated list
          valueInput.setAttribute('type', 'text')
        } else {
          valueInput.setAttribute('step', '.01')
          valueInput.setAttribute('placeholder', '$0.00')
          valueInput.setAttribute('type', 'number')
        }
        break
      case 'LONG':
        if (operatorValue === 'IN' || operatorValue === 'NOT_IN') {
          // IN and NOT_IN operate on lists of longs, which must be entered as a comma-separated list
          valueInput.setAttribute('type', 'text')
        } else {
          valueInput.setAttribute('step', '1')
          valueInput.setAttribute('placeholder', '0')
          valueInput.setAttribute('type', 'number')
        }
        break
      case 'DATE':
        if (
          operatorValue === 'AGE_OLDER_THAN' ||
          operatorValue === 'AGE_YOUNGER_THAN'
        ) {
          // Age-related operators should have number input value
          valueInput.setAttribute('type', 'number')
          // We should allow for decimals to account for month intervals
          valueInput.setAttribute('step', '.01')
        } else if (operatorValue === 'AGE_BETWEEN') {
          // BETWEEN operates on lists of longs, which must be entered as a comma-separated list
          valueInput.setAttribute('type', 'text')
        } else {
          valueInput.setAttribute('type', 'date')
        }
        break
      default:
        valueInput.setAttribute('type', 'text')
    }
  }

  updateNameAndIdForFormControl(el: HTMLInputElement | HTMLSelectElement) {
    const groupNumString = assertNotNull(el.name.match(/group-(\d+)/))[1]

    let groupNum = parseInt(groupNumString, 10)
    el.name = el.name.replace(/group-\d+/, `group-${++groupNum}`)

    // The server-rendered inputs have UUID-generated IDs to ensure uniqueness on the page.
    // We reuse those IDs and add a suffix to likewise ensure uniqueness for the new inputs.
    const newId = `${el.id}-${groupNum}`
    el.id = newId

    el.closest('label')?.setAttribute('for', newId)
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

      this.updateNameAndIdForFormControl(el)
    })

    newRow
      .querySelectorAll('select')
      .forEach((el: HTMLSelectElement) =>
        this.updateNameAndIdForFormControl(el),
      )

    const deleteButtonDiv = assertNotNull(
      newRow.querySelector('.predicate-config-delete-value-row'),
    ) as HTMLElement

    deleteButtonDiv.addEventListener('click', (event: Event) =>
      this.predicateDeleteValueRow(event),
    )
    // The first row which is being used as a template cannot be deleted
    // and is rendered with a hidden delete button.
    deleteButtonDiv.classList.remove('hidden')

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
   *   @param {string} selectedScalarType The type of the selected option.
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
   *   @param {string} selectedScalarType The type of the selected option.
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
      !(selectedScalarType.toLowerCase() in operatorOption.dataset) ||
      (selectedScalarValue === 'SELECTION' &&
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
