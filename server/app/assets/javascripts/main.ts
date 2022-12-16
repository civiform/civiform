/**
 * We're trying to keep the JS pretty minimal for CiviForm, so we're only using it
 * where it's necessary to improve the user experience.
 *
 * Appropriate uses include:
 *  - Visual transitions that aren't possible with just CSS.
 *  - Rare instances in which we need to update a page without refreshing.
 *  - TBD
 */

function attachDropdown(elementId: string) {
  const dropdownId = elementId + '-dropdown'
  const element = document.getElementById(elementId)
  const dropdown = document.getElementById(dropdownId)
  if (dropdown && element) {
    // Attach onclick event to element to toggle dropdown visibility.
    element.addEventListener('click', (e) => {
      e.stopPropagation()
      toggleElementVisibility(dropdownId)
    })

    // Attach onblur event to page to hide dropdown if it wasn't the clicked element.
    document.addEventListener('click', (e) =>
      maybeHideElement(e, dropdownId, elementId),
    )
  }
}

function toggleElementVisibility(id: string) {
  const element = document.getElementById(id)
  if (element) {
    element.classList.toggle('hidden')
  }
}

function maybeHideElement(e: Event, id: string, parentId: string) {
  if (e.target instanceof Element) {
    const parent = document.getElementById(parentId)
    if (parent && !parent.contains(e.target)) {
      const elementToHide = document.getElementById(id)
      if (elementToHide) {
        elementToHide.classList.add('hidden')
      }
    }
  }
}

/**
 * In admin program block edit form - enabling submit button when form is changed or if not empty
 */
function changeUpdateBlockButtonState() {
  const blockEditForm = document.getElementById('block-edit-form')
  const submitButton = document.getElementById('update-block-button')

  const formNameInput = blockEditForm['block-name-input']
  const formDescriptionText = blockEditForm['block-description-textarea']

  if (
    (formNameInput.value !== formNameInput.defaultValue ||
      formDescriptionText.value !== formDescriptionText.defaultValue) &&
    formNameInput.value !== '' &&
    formDescriptionText.value !== ''
  ) {
    submitButton.removeAttribute('disabled')
  } else {
    submitButton.setAttribute('disabled', '')
  }
}

/**
 * Copy the specified hidden template and append it to the end of the parent divContainerId,
 * above the add button (addButtonId).
 * @param {string} inputTemplateId The ID of the input template to clone.
 * @param {string} addButtonId The ID of "add" button to add imput before.
 * @param {string} divContainerId The ID of the divContainer to add imput before.

 */
function addNewInput(
  inputTemplateId: string,
  addButtonId: string,
  divContainerId: string,
) {
  // Copy the answer template and remove ID and hidden properties.
  const newField = document
    .getElementById(inputTemplateId)
    .cloneNode(true) as HTMLElement
  newField.classList.remove('hidden')
  newField.removeAttribute('id')

  // Register the click event handler for the remove button.
  newField.querySelector('[type=button]').addEventListener('click', removeInput)

  // Find the add option button and insert the new option input field before it.
  const button = document.getElementById(addButtonId)
  document.getElementById(divContainerId).insertBefore(newField, button)
}

/**
 * Removes an input field and its associated elements, like the remove button. All
 * elements must be contained in a parent div.
 * @param {Event} event The event that triggered this action.
 */
function removeInput(event: Event) {
  // Get the parent div, which contains the input field and remove button, and remove it.
  const optionDiv = (event.target as Element).parentNode
  optionDiv.parentNode.removeChild(optionDiv)
}

/**
 * If we want to remove an existing element, hide the input div and set disabled to false
 * so the field is submitted.
 * @param {Event} event The event that triggered this action.
 */
function hideInput(event: Event) {
  const inputDiv = (event.target as Element).parentElement
  // Remove 'disabled' so the field is submitted with the form
  inputDiv.querySelector('input').disabled = false
  // Hide the entire div from the user
  inputDiv.classList.add('hidden')
}

/**
 * Remove line-clamp from div on click.
 *
 * NOTE: This is in no way discoverable, but it's just a temporary fix until we have a program
 * landing page.
 * @param {Event} event The event that triggered this action.
 */
function removeLineClamp(event: Event) {
  const target = event.target as HTMLElement
  target.classList.add('line-clamp-none')
}

function attachLineClampListeners() {
  const applicationCardDescriptions = Array.from(
    document.querySelectorAll('.cf-application-card-description'),
  )
  applicationCardDescriptions.forEach((el) =>
    el.addEventListener('click', removeLineClamp),
  )
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

function attachFormDebouncers() {
  Array.from(document.querySelectorAll('.cf-debounced-form')).forEach(
    (formEl) => {
      const submitEl = formEl.querySelector('button[type="submit"]')
      if (!submitEl) {
        return
      }
      // Prevent double-clicks from submitting the form multiple times by
      // disabling the submit button after the initial click.
      formEl.addEventListener('submit', () => {
        submitEl.setAttribute('disabled', '')
      })
    },
  )
}

/**
 * Adds event listener to all elements on a page that match given selector.
 * This function doesn't handle elements added dynamically after the function was invoked.
 * @param {string} selector CSS selector that will be used to retrieve list of elements.
 * @param {string} event Browser event. For example 'click'
 * @param {Function} listener Listener that will be registered on all matching elements.
 */
function addEventListenerToElements(
  selector: string,
  event: string,
  listener: (e: Event) => void,
) {
  Array.from(document.querySelectorAll(selector)).forEach((el) =>
    el.addEventListener(event, listener),
  )
}

/**
 * Adds listeners to all elements that have `data-redirect-to="..."` attribute.
 * All such elements act as links taking user to another page.
 */
function attachRedirectToPageListeners() {
  addEventListenerToElements('[data-redirect-to]', 'click', (e: Event) => {
    e.stopPropagation()
    window.location.href = (e.currentTarget as HTMLElement).dataset.redirectTo
  })
}

/**
 * Adds listeners to buttons with 'form' attributes. These buttons trigger form
 * submission without JS. But we still need to stop propagation of events
 * because it's possible that some other button up-stream in the ancestor chain
 * contains a click listener as we have nested clickable elements.
 */
function attachStopPropogationListenerOnFormButtons() {
  addEventListenerToElements('button[form]', 'click', (e: Event) => {
    e.stopPropagation()
  })
}

/**
 * Disables default browser behavior where pressing Enter on any input in a form
 * triggers form submission. See https://github.com/civiform/civiform/issues/3872
 */
function disableEnterToSubmitBehaviorOnForms() {
  addEventListenerToElements('form', 'keydown', (e: KeyboardEvent) => {
    const target = (e.target as HTMLElement).tagName.toLowerCase()
    // if event originated from a button or link - it should proceed with
    // default action.
    if (target !== 'button' && target !== 'a' && e.key === 'Enter') {
      e.preventDefault()
    }
  })
}

function predicateAddValueRow(event: Event) {
  event.preventDefault()
  event.stopPropagation()

  const currentRows = document.querySelectorAll(
    '.predicate-config-value-row',
  )
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

  newRow
    .querySelectorAll('.predicate-config-delete-value-row')
    .forEach((el) => {
      el.addEventListener('click', predicateDeleteValueRow)
      el.classList.remove('hidden')
    })

  document.getElementById('predicate-config-value-row-container').append(newRow)
}

function predicateDeleteValueRow(event: Event) {
  event.preventDefault()
  event.stopPropagation()

  const valueRow = (event.target as HTMLElement).closest(
    '.predicate-config-value-row',
  )
  valueRow.remove()
}

window.addEventListener('load', () => {
  attachDropdown('create-question-button')
  Array.from(document.querySelectorAll('.cf-with-dropdown')).forEach((el) => {
    attachDropdown(el.id)
  })

  attachLineClampListeners()

  // Configure the admin predicate builder to show the appropriate options based on
  // the type of scalar selected.
  addEventListenerToElements(
    '.cf-scalar-select',
    'input',
    configurePredicateFormOnScalarChange,
  )

  addEventListenerToElements(
    '.cf-operator-select',
    'input',
    configurePredicateFormOnOperatorChange,
  )

  document
    .querySelector('#predicate-add-value-set')
    ?.addEventListener('click', predicateAddValueRow)

  addEventListenerToElements(
    'predicate-config-delete-value-row',
    'click',
    predicateDeleteValueRow,
  )

  // Submit button is disabled by default until program block edit form is changed
  const blockEditForm = document.getElementById('block-edit-form')
  if (blockEditForm) {
    blockEditForm.addEventListener('input', changeUpdateBlockButtonState)
  }

  // Configure the button on the admin question form to add more answer options
  const questionOptionButton = document.getElementById('add-new-option')
  if (questionOptionButton) {
    questionOptionButton.addEventListener('click', function () {
      addNewInput(
        'multi-option-question-answer-template',
        'add-new-option',
        'question-settings',
      )
    })
  }

  // Bind click handler for remove options in multi-option edit view
  addEventListenerToElements(
    '.multi-option-question-field-remove-button',
    'click',
    removeInput,
  )

  // Configure the button on the manage program admins form to add more email inputs
  const adminEmailButton = document.getElementById('add-program-admin-button')
  if (adminEmailButton) {
    adminEmailButton.addEventListener('click', function () {
      addNewInput(
        'program-admin-email-template',
        'add-program-admin-button',
        'program-admin-emails',
      )
    })
  }

  // Bind click handler for removing program admins in the program admin management view
  addEventListenerToElements(
    '.cf-program-admin-remove-button',
    'click',
    hideInput,
  )

  attachFormDebouncers()

  attachRedirectToPageListeners()
  attachStopPropogationListenerOnFormButtons()
  disableEnterToSubmitBehaviorOnForms()

  // Advertise (e.g., for browser tests) that main.ts initialization is done
  document.body.dataset.loadMain = 'true'
})
