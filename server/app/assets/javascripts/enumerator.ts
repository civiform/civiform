// Javascript handling for enumerators
// This file requires that main.ts is also added to the page.

import {addEventListenerToElements, assertNotNull} from '@/util'

// Inputs across the visible repeated entity rows whose emptiness disables the "Add" button. The
// two paths contribute different inputs: legacy (free-text) rows tag their single text input with
// data-entity-input, while initial-question rows contribute only their required controls (aria-required).
const ADD_BUTTON_GATING_INPUTS_SELECTOR = [
  '.cf-question-enumerator .cf-enumerator-field:not(.display-none) input[data-entity-input]',
  '.cf-question-enumerator .cf-enumerator-field:not(.display-none) input[aria-required="true"]',
  '.cf-question-enumerator .cf-enumerator-field:not(.display-none) select[aria-required="true"]',
  '.cf-question-enumerator .cf-enumerator-field:not(.display-none) textarea[aria-required="true"]',
].join(', ')

const VISIBLE_ENTITY_FIELDS_SELECTOR =
  '.cf-question-enumerator .cf-enumerator-field:not(.display-none)'

export function init() {
  updateListeners()

  // After HTMX swaps a new entity row into #enumerator-fields, re-attach listeners to the new
  // controls and move focus into the new row.
  document.addEventListener('htmx:afterSettle', (event) => {
    if (event.detail.target.id === 'enumerator-fields') {
      updateListeners()
      focusFirstInputInLastEntity()
    }
  })
}

/** Safe to call multiple times */
export function updateListeners() {
  refreshAddButtonStatus()

  addEventListenerToElements(
    ADD_BUTTON_GATING_INPUTS_SELECTOR,
    'input',
    refreshAddButtonStatus,
  )

  // Only attach the JS add handler for the legacy add button. The initial-question add button is
  // handled by HTMX plus the htmx:afterSettle listener above.
  const addButton = document.getElementById('enumerator-field-add-button')
  if (addButton && !isInitialQuestionAddButton(addButton)) {
    addButton.addEventListener('click', addNewEnumeratorField)
  }

  addEventListenerToElements(
    '.cf-enumerator-delete-button',
    'click',
    removeEnumeratorField,
  )
}

// Used to allow us to generate a unique ID for each newly added enumerator entity.
// We can't generate this based on the number of elements rendered since entities
// can be dynamically removed by using the "Remove" button without refreshing the
// page.
let enumeratorCounter = 0

/** In the enumerator form - add a new input field for a repeated entity (legacy text input mode). */
function addNewEnumeratorField() {
  // Copy the enumerator field template
  const newField = assertNotNull(
    document.getElementById('enumerator-field-template'),
  ).cloneNode(true) as HTMLElement
  newField.classList.remove('display-none')
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
    document.querySelectorAll('.cf-enumerator-field:not(.display-none)'),
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
 * Remove an enumerator entity. Saved entities (rendered from the server with a data-entity-saved
 * marker) are hidden and a delete marker is submitted so the server removes them; unsaved entities
 * (added client-side, e.g. via HTMX) are simply removed from the DOM.
 * @param {Event} event The event that triggered this action.
 */
function removeEnumeratorField(event: Event) {
  const removeButton = event.currentTarget as HTMLElement
  if (!confirm(removeButton.dataset.confirmationMessage)) {
    return false
  }

  const enumeratorFieldDiv = findEnumeratorFieldDiv(removeButton)

  if (enumeratorFieldDiv.hasAttribute('data-entity-saved')) {
    // Saved entity: hide the field (rather than removing it) and submit a delete marker so the
    // server knows to remove it. Hiding it also excludes its inputs from the add-button checks.
    enumeratorFieldDiv.classList.add('display-none')

    // Create a copy of the hidden deleted entity template. Set the value to this button's ID (the
    // entity index), and enable it so the data is submitted with the form.
    const deletedEntityInput = assertNotNull(
      document.getElementById('enumerator-delete-template'),
    ).cloneNode(true) as HTMLInputElement
    deletedEntityInput.disabled = false
    deletedEntityInput.setAttribute('value', removeButton.id)
    deletedEntityInput.removeAttribute('id')
    enumeratorFieldDiv.appendChild(deletedEntityInput)
  } else {
    // Unsaved entity: simply remove it from the DOM.
    enumeratorFieldDiv.remove()
  }

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
    '.cf-enumerator-field:not(.display-none) .cf-enumerator-delete-button',
  )
  if (deleteButtons.length === 0) {
    // Set focus to add button
    document.getElementById('enumerator-field-add-button')!.focus()
  } else {
    // Set focus to last remove button
    ;(deleteButtons[deleteButtons.length - 1] as HTMLElement).focus()
  }
}

/** Focus the first input in the last (newly HTMX-added) entity row. */
function focusFirstInputInLastEntity() {
  const fields = document.querySelectorAll(
    '#enumerator-fields .cf-enumerator-field:not(.display-none)',
  )
  if (fields.length === 0) return

  const lastField = fields[fields.length - 1]
  const firstInput = lastField.querySelector<HTMLElement>(
    'input:not([type=hidden]), select, textarea',
  )
  if (firstInput) {
    firstInput.focus()
  }
}

/**
 * Enable the add button if and only if all required inputs are filled (the user doesn't need two
 * blank inputs) and the user has not reached the maximum number of entities. For initial-question
 * (HTMX) rows, the last row must also have at least one value, so the user can't stack up empty
 * rows even when the initial question is optional (its inputs are not aria-required).
 */
function refreshAddButtonStatus() {
  const hasEmptyGatingInput = Array.from(
    document.querySelectorAll(ADD_BUTTON_GATING_INPUTS_SELECTOR),
  ).some((item) => (item as HTMLInputElement).value === '')

  const addButton = document.getElementById(
    'enumerator-field-add-button',
  ) as HTMLInputElement | null
  if (!addButton) return

  const isInitialQuestion = isInitialQuestionAddButton(addButton)

  const entityCount = document.querySelectorAll(
    VISIBLE_ENTITY_FIELDS_SELECTOR,
  ).length

  // Initial-question rows may be fully optional, so the gating-input check alone wouldn't stop the
  // user from stacking blank rows. Require the last row to have at least one value.
  const lastRowEmpty = isInitialQuestion && isLastEntityRowEmpty()

  // converts to 0 or NaN if unset
  const maxEntities = Number(addButton.dataset.maxEntities)
  addButton.disabled =
    hasEmptyGatingInput ||
    lastRowEmpty ||
    (maxEntities > 0 && entityCount >= maxEntities)

  // For the initial-question (HTMX) button, pass the current entityCount so the server assigns the
  // next entity the correct index.
  if (isInitialQuestion) {
    addButton.setAttribute('hx-vals', JSON.stringify({entityCount}))
  }
}

/**
 * Returns true if the last visible entity row has no value in any of its inputs. Used to keep the
 * add button disabled until the current (last) row has at least one value.
 */
function isLastEntityRowEmpty(): boolean {
  const fields = document.querySelectorAll(VISIBLE_ENTITY_FIELDS_SELECTOR)
  if (fields.length === 0) return false

  const lastField = fields[fields.length - 1]
  const valueInputs = lastField.querySelectorAll<HTMLInputElement>(
    'input:not([type=hidden]), select, textarea',
  )
  return Array.from(valueInputs).every((input) => input.value === '')
}

/**
 * When enumerator entities are removed from the page, we need to repaint all
 * labels and buttons so the index orders remain correct.
 */
function repaintAllLabelsAndButtons() {
  const enumeratorFields = document.querySelectorAll(
    VISIBLE_ENTITY_FIELDS_SELECTOR,
  )
  enumeratorFields.forEach((field, index) => {
    addIndexToLabelAndButton(field, index)
    repointEntityAria(field, index)
  })
}

/**
 * When enumerator entities are added or removed from the page we need to repaint
 * the label and remove-button text to update the visible index.
 * @param {Element} field The element containing the button and label to be relabeled
 * @param {number} index The zero-based position of the entity in the visible list
 */
function addIndexToLabelAndButton(field: Element, index: number) {
  const indexString = ` #${index + 1}`

  // The entity label: initial-question rows mark it with .cf-entity-index-label; legacy rows use
  // the label inside .cf-entity-name-input.
  const label =
    field.querySelector<HTMLElement>('.cf-entity-index-label') ??
    field.querySelector<HTMLElement>('.cf-entity-name-input label')
  if (label) {
    label.innerText = replaceIndexSuffix(label.innerText, indexString)
  }

  const deleteButton = field.querySelector<HTMLElement>(
    '.cf-enumerator-delete-button',
  )
  if (deleteButton) {
    deleteButton.innerText = replaceIndexSuffix(
      deleteButton.innerText,
      indexString,
    )
  }
}

/** Replace a trailing " #<n>" index suffix (if present) with the given one. */
function replaceIndexSuffix(text: string, indexString: string): string {
  return text.replace(/ #\d+$/, '') + indexString
}

/**
 * Re-point the enumerator title/help references after entities are re-indexed. Only the first
 * initial-question entity row references the enumerator title (aria-labelledby) and help text
 * (aria-describedby); subsequent rows reference only their own entity label. When the first row is
 * removed, its successor must take over those references. Legacy (free-text) rows use a fieldset
 * for the enumerator title and are skipped.
 * @param {Element} field The entity row.
 * @param {number} index The zero-based position of the entity in the visible list.
 */
function repointEntityAria(field: Element, index: number) {
  const label = field.querySelector<HTMLElement>('.cf-entity-index-label')
  if (label === null) {
    // Legacy row: the enumerator title lives on the surrounding fieldset, not per row.
    return
  }

  const root = field.closest('.cf-question-enumerator')
  if (root === null || root.id === '') return

  const titleId = `${root.id}-title`
  const descriptionId = `${root.id}-description`
  const labelId = label.id

  // Multi-input rows carry aria on the role="group" wrapper; single-input rows carry it on the
  // control, which is named natively by its <label for> when it is not the first row.
  const isGroup = field.getAttribute('role') === 'group'
  const ariaTarget = isGroup
    ? (field as HTMLElement)
    : field.querySelector<HTMLElement>('input, select, textarea')
  if (ariaTarget === null) return

  if (index === 0) {
    ariaTarget.setAttribute('aria-labelledby', `${titleId} ${labelId}`)
    ariaTarget.setAttribute('aria-describedby', descriptionId)
  } else if (isGroup) {
    ariaTarget.setAttribute('aria-labelledby', labelId)
    ariaTarget.removeAttribute('aria-describedby')
  } else {
    ariaTarget.removeAttribute('aria-labelledby')
    ariaTarget.removeAttribute('aria-describedby')
  }
}

/**
 * Returns true if the add button drives the initial-question flow. Only that button is HTMX-powered
 * (has an hx-post attribute); the legacy add button is not.
 * @param {HTMLElement} addButton the enumerator add button.
 */
function isInitialQuestionAddButton(addButton: HTMLElement): boolean {
  return addButton.hasAttribute('hx-post')
}

/**
 * Find the enumerator field div from a child element.
 * @param {Element} element a child element of the enumerator field.
 */
function findEnumeratorFieldDiv(element: HTMLElement) {
  let enumeratorFieldDiv = assertNotNull(element.parentElement)
  // The parent div may be one level above, so we check to make sure the class is correct.
  while (
    enumeratorFieldDiv &&
    !enumeratorFieldDiv.classList.contains('cf-enumerator-field')
  ) {
    enumeratorFieldDiv = assertNotNull(enumeratorFieldDiv.parentElement)
  }
  return enumeratorFieldDiv
}
