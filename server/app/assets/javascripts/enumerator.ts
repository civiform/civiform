// Javascript handling for enumerators
// This file requires that main.ts is also added to the page.

import {addEventListenerToElements, assertNotNull} from './util'

export function init() {
  updateListeners()
}

/** Safe to call multiple times */
export function updateListeners() {
  refreshAddButtonStatus()

  addEventListenerToElements(
    '.cf-question-enumerator input[data-entity-input]',
    'input',
    refreshAddButtonStatus,
  )

  addEventListenerToElements(
    '#enumerator-field-add-button',
    'click',
    addNewEnumeratorField,
  )

  addEventListenerToElements(
    '.cf-enumerator-delete-button',
    'click',
    removeExistingEnumeratorField,
  )
}

// Used to allow us to generate a unique ID for each newly added enumerator entity.
// We can't generate this based on the number of elements rendered since entities
// can be dynamically removed by using the "Remove" button without refreshing the
// page.
let enumeratorCounter = 0

/** In the enumerator form - add a new input field for a repeated entity. */
function addNewEnumeratorField() {
  // Copy the enumerator field template
  const newField = assertNotNull(
    document.getElementById('enumerator-field-template'),
  ).cloneNode(true) as HTMLElement
  newField.classList.remove('hidden')
  newField.removeAttribute('id')

  // Add the remove enumerator field event listener to the delete button
  newField
    .querySelector('[type=button]')!
    .addEventListener('click', removeEnumeratorField)

  // Update IDs from the template element so that they are unique.
  const inputElement = assertNotNull(
    newField.querySelector('#enumerator-field-template-input'),
  )
  enumeratorCounter++
  inputElement.id = `${inputElement.id}-${enumeratorCounter}`
  const labelElement = assertNotNull(
    newField.querySelector('label[for="enumerator-field-template-input"]'),
  )
  labelElement.setAttribute('for', inputElement.id)

  // every time we add a new input, we need to add the index number to the label and button
  const index = assertNotNull(
    document.querySelectorAll('.cf-enumerator-field:not(.hidden)'),
  ).length
  addIndexToLabelAndButton(newField, index)

  const errorsElement = assertNotNull(
    newField.querySelector('#enumerator-field-template-input-errors'),
  )
  errorsElement.id = `enumerator-field-template-input-${enumeratorCounter}-errors`

  // Add to the end of enumerator-fields div.
  const enumeratorFields = assertNotNull(
    document.getElementById('enumerator-fields'),
  )
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

  newInput.addEventListener('input', refreshAddButtonStatus)
  refreshAddButtonStatus()
}

/**
 * Remove an entity that was added client-side and has not been saved to the server.
 * @param {Event} event The event that triggered this action.
 */
function removeEnumeratorField(event: Event) {
  const removeButton = event.currentTarget as HTMLElement
  if (!confirm(removeButton.dataset.confirmationMessage)) {
    return false
  }

  // Get the parent div, which contains the input field and remove button, and remove it.
  const enumeratorFieldDiv = findEnumeratorFieldDiv(removeButton)
  enumeratorFieldDiv.remove()

  // Need to re-index all enumerator entities when one is removed so labels are consistent
  repaintAllLabelsAndButtons()
  refreshAddButtonStatus()
  setFocusAfterEnumeratorRemoval()
}

/**
 * Remove an entity that has been saved to the server. The server needs to know the entity was
 * deleted, so we can't just delete it from the DOM.
 * @param {Event} event The event that triggered this action.
 */
function removeExistingEnumeratorField(event: Event) {
  // Get the button that was clicked
  const removeButton = event.currentTarget as HTMLElement

  if (!confirm(removeButton.dataset.confirmationMessage)) {
    return false
  }

  // Hide the field that was removed. We cannot remove it completely, as we need to
  // submit the input to maintain entity ordering.
  const enumeratorFieldDiv = findEnumeratorFieldDiv(removeButton)
  enumeratorFieldDiv.classList.add('hidden')
  // We must hide the child in addition to the parent since we
  // want to prevent this input from being considered when
  // toggling whether the "Add" button is enabled (especially if
  // the applicant were removing a blank input).
  const enumeratorInput = assertNotNull(
    enumeratorFieldDiv.querySelector('input'),
  )
  enumeratorInput.classList.add('hidden')
  enumeratorInput.removeAttribute('data-entity-input')

  // Create a copy of the hidden deleted entity template. Set the value to this
  // button's ID, and set disabled to false so the data is submitted with the form.
  const deletedEntityInput = assertNotNull(
    document.getElementById('enumerator-delete-template'),
  ).cloneNode(true) as HTMLInputElement
  deletedEntityInput.disabled = false
  deletedEntityInput.setAttribute('value', removeButton.id)
  deletedEntityInput.removeAttribute('id')

  // Add the hidden deleted entity input to the page.
  enumeratorFieldDiv.appendChild(deletedEntityInput)

  // Need to re-index all enumerator entities when one is removed so labels are consistent
  repaintAllLabelsAndButtons()
  refreshAddButtonStatus()
  setFocusAfterEnumeratorRemoval()
}

/**
 * Set focus after an enumerator entity is removed. If no other entries are
 * present, will set to the add button, otherwise will set to the last entity's
 * remove button. Setting focus is important for a11y (e.g. for keyboard only
 * or screenreader users).
 */
function setFocusAfterEnumeratorRemoval() {
  const deleteButtons = document.querySelectorAll(
    '.cf-enumerator-field:not(.hidden) .cf-enumerator-delete-button',
  )
  if (deleteButtons.length === 0) {
    // Set focus to add button
    document.getElementById('enumerator-field-add-button')!.focus()
  } else {
    // Set focus to last remove button
    ;(deleteButtons[deleteButtons.length - 1] as HTMLElement).focus()
  }
}

/**
 * Enable the add button if and only if all inputs are filled (the user doesn't need two blank
 * inputs) and the user has not reached the maximum number of inputs.
 */
function refreshAddButtonStatus() {
  const enumeratorInputValues = Array.from(
    document.querySelectorAll(
      '.cf-question-enumerator input[data-entity-input]',
    ),
  ).map((item) => (item as HTMLInputElement).value)

  // validate that there are no empty inputs.
  const addButton = document.getElementById(
    'enumerator-field-add-button',
  ) as HTMLInputElement

  if (addButton) {
    // converts to 0 or NaN if unset
    const maxEntities = Number(addButton.dataset.maxEntities)
    addButton.disabled =
      enumeratorInputValues.includes('') ||
      (maxEntities > 0 && enumeratorInputValues.length >= maxEntities)
  }
}

/**
 * When enumerator entities are removed from the page, we need to repaint all
 * labels and buttons so the index orders remain correct.
 */
function repaintAllLabelsAndButtons() {
  const enumeratorFields = assertNotNull(
    document.querySelectorAll('.cf-enumerator-field:not(.hidden)'),
  )
  enumeratorFields.forEach((field, index) => {
    addIndexToLabelAndButton(field, index)
  })
}

/**
 * When enumerator entities are added or removed from the page we need to repaint
 * the label and button text to update the index
 * @param {Element} field The element containing the button and label to be relabeled
 * @param {number} index The index to add to the button and label
 */
function addIndexToLabelAndButton(field: Element, index: number) {
  const indexString = ` #${index + 1}`
  const labelBaseText = assertNotNull(
    document.querySelector('div[data-label-text]'),
  ).getAttribute('data-label-text')
  const labelElement = assertNotNull(field.querySelector('label'))
  labelElement.innerText = labelBaseText ? labelBaseText + indexString : ''

  const buttonBaseText = assertNotNull(
    document.querySelector('div[data-button-text]'),
  ).getAttribute('data-button-text')
  const buttonElement = assertNotNull(field.querySelector('button'))
  buttonElement.innerText = buttonBaseText ? buttonBaseText + indexString : ''
}

/**
 * Find the enumerator field div from a child element.
 * @param {Element} element a child element of the enumerator field.
 */
function findEnumeratorFieldDiv(element: HTMLElement) {
  let enumeratorFieldDiv = assertNotNull(element.parentElement)
  // The parent div may be one level above (for north star), so we check to make sure the class is correct.
  while (
    enumeratorFieldDiv &&
    !enumeratorFieldDiv.classList.contains('cf-enumerator-field')
  ) {
    enumeratorFieldDiv = assertNotNull(enumeratorFieldDiv.parentElement)
  }
  return enumeratorFieldDiv
}
