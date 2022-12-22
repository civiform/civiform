/**
 * We're trying to keep the JS pretty minimal for CiviForm, so we're only using it
 * where it's necessary to improve the user experience.
 *
 * Appropriate uses include:
 *  - Visual transitions that aren't possible with just CSS.
 *  - Rare instances in which we need to update a page without refreshing.
 *  - TBD
 */

import {addEventListenerToElements} from './dom_utils'

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
  const optionDiv = (event.currentTarget as Element).parentNode
  optionDiv.parentNode.removeChild(optionDiv)
}

/**
 * If we want to remove an existing element, hide the input div and set disabled to false
 * so the field is submitted.
 * @param {Event} event The event that triggered this action.
 */
function hideInput(event: Event) {
  const inputDiv = (event.currentTarget as Element).parentElement
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

export function init() {
  attachDropdown('create-question-button')
  Array.from(document.querySelectorAll('.cf-with-dropdown')).forEach((el) => {
    attachDropdown(el.id)
  })

  attachLineClampListeners()

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
}
