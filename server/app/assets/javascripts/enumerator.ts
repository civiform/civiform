// Javascript handling for enumerators
// This file requires that main.ts is also added to the page.

import {addEventListenerToElements} from './util'

export function init() {
  // Configure the button on the enumerator question form to add more enumerator field options
  const enumeratorOptionButton = document.getElementById(
    'enumerator-field-add-button',
  )
  if (enumeratorOptionButton) {
    enumeratorOptionButton.addEventListener('click', addNewEnumeratorField)
  }

  addEventListenerToElements(
    '.cf-enumerator-delete-button',
    'click',
    removeExistingEnumeratorField,
  )
  addEnumeratorListeners()
}

// Used to allow us to generate a unique ID for each newly added enumerator entity.
// We can't generate this based on the number of elements rendered since entities
// can be dynamically removed by using the "Remove" button without refreshing the
// page.
let enumeratorCounter = 0

/** In the enumerator form - add a new input field for a repeated entity. */
function addNewEnumeratorField() {
  // Copy the enumerator field template
  const newField = document
    .getElementById('enumerator-field-template')
    .cloneNode(true) as HTMLElement
  newField.classList.remove('hidden')
  newField.removeAttribute('id')

  // Add the remove enumerator field event listener to the delete button
  newField
    .querySelector('[type=button]')
    .addEventListener('click', removeEnumeratorField)

  // Update IDs from the template element so that they are unique.
  const inputElement = newField.querySelector(
    '#enumerator-field-template-input',
  )
  enumeratorCounter++
  inputElement.id = `${inputElement.id}-${enumeratorCounter}`
  const labelElement = newField.querySelector(
    'label[for="enumerator-field-template-input"]',
  )
  labelElement.setAttribute('for', inputElement.id)
  const errorsElement = newField.querySelector(
    '#enumerator-field-template-input-errors',
  )
  errorsElement.id = `enumerator-field-template-input-${enumeratorCounter}-errors`

  // Add to the end of enumerator-fields div.
  const enumeratorFields = document.getElementById('enumerator-fields')
  enumeratorFields.appendChild(newField)

  // Set focus to the new input
  const newInput = newField.querySelector('input')
  if (!newInput) {
    throw new Error(
      'Expected an input associated with the new enumerator entity',
    )
  }
  newInput.setAttribute('data-entity-input', '')
  // Set disabled to false so the data is submitted with the form.
  newInput.disabled = false
  newInput.focus()
}

/**
 * In the enumerator form - remove a input field for a repeated entity
 * @param {Event} event The event that triggered this action.
 */
function removeEnumeratorField(event: Event) {
  // Get the parent div, which contains the input field and remove button, and remove it.
  const enumeratorFieldDiv = (event.currentTarget as HTMLElement).parentNode
  enumeratorFieldDiv.parentNode.removeChild(enumeratorFieldDiv)
}

/**
 * Enumerator delete buttons for existing entities behave differently than removing fields that
 * were just added client-side and were not saved server-side.
 * @param {Event} event The event that triggered this action.
 */
function removeExistingEnumeratorField(event: Event) {
  // Get the button that was clicked
  const removeButton = event.currentTarget as HTMLElement

  // Hide the field that was removed. We cannot remove it completely, as we need to
  // submit the input to maintain entity ordering.
  const enumeratorFieldDiv = removeButton.parentElement
  enumeratorFieldDiv.classList.add('hidden')
  // We must hide the child in addition to the parent since we
  // want to prevent this input from being considered when
  // toggling whether the "Add" button is enabled (especially if
  // the applicant were removing a blank input).
  const enumeratorInput = enumeratorFieldDiv.querySelector('input')
  enumeratorInput.classList.add('hidden')

  // Create a copy of the hidden deleted entity template. Set the value to this
  // button's ID, and set disabled to false so the data is submitted with the form.
  const deletedEntityInput = document
    .getElementById('enumerator-delete-template')
    .cloneNode(true) as HTMLInputElement
  deletedEntityInput.disabled = false
  deletedEntityInput.setAttribute('value', removeButton.id)
  deletedEntityInput.removeAttribute('id')

  // Add the hidden deleted entity input to the page.
  enumeratorFieldDiv.appendChild(deletedEntityInput)
}

/** Add listeners to all enumerator inputs to update validation on changes. */
function addEnumeratorListeners() {
  // Assumption: There is only ever zero or one enumerators per block.
  const enumeratorQuestion = document.querySelector('.cf-question-enumerator')
  if (!enumeratorQuestion) {
    return
  }
  const enumeratorInputs = Array.from(
    enumeratorQuestion.querySelectorAll('input[data-entity-input]'),
  )
  // Whenever an input changes we need to revalidate.
  enumeratorInputs.forEach((enumeratorInput) => {
    enumeratorInput.addEventListener('input', () => {
      maybeHideEnumeratorAddButton(enumeratorQuestion)
    })
  })

  // Whenever an input is added, we need to add a change listener.
  const mutationObserver = new MutationObserver((records: MutationRecord[]) => {
    for (const record of records) {
      for (const newNode of Array.from(record.addedNodes)) {
        const newInputs = Array.from(
          (<Element>newNode).querySelectorAll('input'),
        )
        newInputs.forEach((newInput) => {
          newInput.addEventListener('input', () => {
            maybeHideEnumeratorAddButton(enumeratorQuestion)
          })
        })
      }
    }
    maybeHideEnumeratorAddButton(enumeratorQuestion)
  })

  mutationObserver.observe(enumeratorQuestion, {
    childList: true,
    subtree: true,
    characterDataOldValue: true,
  })
}

/** If we have empty inputs then disable the add input button. (We don't need two blank inputs.)
 * @param {Element} enumeratorQuestion The question to hide/show the add button for.
 */
function maybeHideEnumeratorAddButton(enumeratorQuestion: Element) {
  if (enumeratorQuestion) {
    const enumeratorInputValues = Array.from(
      enumeratorQuestion.querySelectorAll('input[data-entity-input]'),
    ).map((item) => (item as HTMLInputElement).value)

    // validate that there are no empty inputs.
    const addButton = <HTMLInputElement>(
      document.getElementById('enumerator-field-add-button')
    )
    if (addButton) {
      addButton.disabled = enumeratorInputValues.includes('')
    }
  }
}
