// Javascript handling for enumerators
// This file requires that main.ts is also added to the page.

import {addEventListenerToElements, assertNotNull} from './util'

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
}

/**
 * In the enumerator form - remove a input field for a repeated entity
 * @param {Event} event The event that triggered this action.
 */
function removeEnumeratorField(event: Event) {
  // Get the parent div, which contains the input field and remove button, and remove it.
  const enumeratorFieldDiv = assertNotNull(
    (event.currentTarget as HTMLElement).parentNode,
  ) as HTMLElement
  enumeratorFieldDiv.remove()

  // Need to re-index all enumerator entities when one is removed so labels are consistent
  repaintAllLabelsAndButtons()

  setFocusAfterEnumeratorRemoval()
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
  const enumeratorFieldDiv = assertNotNull(removeButton.parentElement)
  enumeratorFieldDiv.classList.add('hidden')
  // We must hide the child in addition to the parent since we
  // want to prevent this input from being considered when
  // toggling whether the "Add" button is enabled (especially if
  // the applicant were removing a blank input).
  const enumeratorInput = assertNotNull(
    enumeratorFieldDiv.querySelector('input'),
  )
  enumeratorInput.classList.add('hidden')

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
    // No entries, set focus to add button.
    const enumeratorQuestion = assertNotNull(
      document.querySelector('.cf-question-enumerator'),
    )
    // Enable button before setting focus. The mutation observer that sets this
    // isn't guaranteed to run first.
    maybeToggleEnumeratorAddButton(enumeratorQuestion)
    const addButton = assertNotNull(
      document.getElementById('enumerator-field-add-button'),
    )
    addButton.focus()
  } else {
    // Other entries, set to last remove button.
    ;(deleteButtons[deleteButtons.length - 1] as HTMLElement).focus()
  }
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
      maybeToggleEnumeratorAddButton(enumeratorQuestion)
    })
  })

  // Whenever an input is added, we need to add a change listener.
  const mutationObserver = new MutationObserver((records: MutationRecord[]) => {
    for (const record of records) {
      for (const newNode of Array.from(record.addedNodes)) {
        // changes to the label and button texts trigger the mutationObserver which results in an error
        // this if statement protects against that case
        if ((<Element>newNode).querySelectorAll) {
          const newInputs = Array.from(
            (<Element>newNode).querySelectorAll('input'),
          )
          newInputs.forEach((newInput) => {
            newInput.addEventListener('input', () => {
              maybeToggleEnumeratorAddButton(enumeratorQuestion)
            })
          })
        }
      }
    }
    maybeToggleEnumeratorAddButton(enumeratorQuestion)
  })

  mutationObserver.observe(enumeratorQuestion, {
    childList: true,
    subtree: true,
    characterDataOldValue: true,
  })
}

/**
 * Disable the add button if there are empty inputs, re-enable the add button otherwise.
 * (We don't need two blank inputs.)
 * @param {Element} enumeratorQuestion The question to hide/show the add button for.
 */
function maybeToggleEnumeratorAddButton(enumeratorQuestion: Element) {
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
 * @param {Element} field The element comtaining the button and label to be relabled
 * @param {number} index The index to add to the button and label
 */
function addIndexToLabelAndButton(field: Element, index: number) {
  const indexString = ` #${index + 1}`
  const labelBaseText = assertNotNull(
    document.querySelector('div[data-label-text]'),
  ).getAttribute('data-label-text')
  const labelElement = assertNotNull(field.querySelector('label'))
  labelElement.innerHTML = labelBaseText ? labelBaseText + indexString : ''

  const buttonBaseText = assertNotNull(
    document.querySelector('div[data-button-text]'),
  ).getAttribute('data-button-text')
  const buttonElement = assertNotNull(field.querySelector('button'))
  buttonElement.innerHTML = buttonBaseText ? buttonBaseText + indexString : ''
}
