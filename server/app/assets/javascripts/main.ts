/**
 * We're trying to keep the JS pretty minimal for CiviForm, so we're only using it
 * where it's necessary to improve the user experience.
 *
 * Appropriate uses include:
 *  - Visual transitions that aren't possible with just CSS.
 *  - Rare instances in which we need to update a page without refreshing.
 *  - TBD
 */

import {addEventListenerToElements, assertNotNull} from './util'

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
  const blockEditForm = assertNotNull(
    document.getElementById('block-edit-form'),
  )
  const submitButton = assertNotNull(
    document.getElementById('update-block-button'),
  )

  const formNameInput = assertNotNull(
    blockEditForm.querySelector<HTMLInputElement>('#block-name-input'),
  )
  const formDescriptionText = assertNotNull(
    blockEditForm.querySelector<HTMLTextAreaElement>(
      '#block-description-textarea',
    ),
  )

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
    .getElementById(inputTemplateId)!
    .cloneNode(true) as HTMLElement
  newField.classList.remove('hidden')
  newField.removeAttribute('id')

  // Register the click event handler for the buttons.
  const deleteButton = newField.querySelector(
    '.multi-option-question-field-remove-button',
  )
  if (deleteButton != null) {
    deleteButton.addEventListener('click', removeInput)
  }
  const upButton = newField.querySelector(
    '.multi-option-question-field-move-up-button',
  )
  if (upButton != null) {
    upButton.addEventListener('click', moveMultiOptionQuestionUp)
  }
  const downButton = newField.querySelector(
    '.multi-option-question-field-move-down-button',
  )
  if (downButton != null) {
    downButton.addEventListener('click', moveMultiOptionQuestionDown)
  }

  // Find the add option button and insert the new option input field before it.
  const button = document.getElementById(addButtonId)
  document.getElementById(divContainerId)!.insertBefore(newField, button)
}

/**
 * Removes an input field and its associated elements, like the remove button. All
 * elements must be contained in a parent div.
 * @param {Event} event The event that triggered this action.
 */
function removeInput(event: Event) {
  // Get the parent div, which contains the input field and remove button, and remove it.
  const clickedOption = assertNotNull(
    (event.currentTarget as Element).parentNode,
  ) as HTMLElement
  clickedOption.remove()
}

/**
 * Swaps an input field and its associated elements with the one above it, if possible. All
 * elements must be contained in a parent div.
 * @param {Event} event The event that triggered this action.
 */
function moveMultiOptionQuestionUp(event: Event) {
  event.preventDefault()
  const clickedOption = assertNotNull(
    (event.currentTarget as Element).parentNode,
  ) as HTMLElement

  const parentContainer = clickedOption.parentElement
  if (parentContainer == null) {
    return
  }

  const options = Array.from(
    document.querySelectorAll(
      'div.cf-multi-option-question-option-editable:not(.hidden)',
    ),
  )
  const clickedIndex = options.indexOf(clickedOption)

  // If the clicked element is not the first element in the parent container,
  // then move it up one index.
  if (clickedIndex > 0) {
    const previousElement = options[clickedIndex - 1]
    parentContainer.insertBefore(clickedOption, previousElement)
  }
}

/**
 * Swaps an input field and its associated elements with the one below it, if possible. All
 * elements must be contained in a parent div.
 * @param {Event} event The event that triggered this action.
 */
function moveMultiOptionQuestionDown(event: Event) {
  event.preventDefault()
  const clickedOption = assertNotNull(
    (event.currentTarget as Element).parentNode,
  ) as HTMLElement

  const parentContainer = clickedOption.parentElement
  if (parentContainer == null) {
    return
  }

  const options = Array.from(
    document.querySelectorAll(
      'div.cf-multi-option-question-option-editable:not(.hidden)',
    ),
  )
  const clickedIndex = options.indexOf(clickedOption)

  // If the clicked element is not the last element in the parent container,
  // then move it down one index.
  if (clickedIndex < options.length - 1) {
    const nextElement = options[clickedIndex + 1]
    parentContainer.insertBefore(nextElement, clickedOption)
  }
}

/**
 * Remove line-clamp from div on click.
 *
 * NOTE: This is in no way discoverable, but it's just a temporary fix until we have a program
 * landing page.
 * @param {Event} event The event that triggered this action.
 */
function removeLineClamp(event: Event) {
  const div = event.currentTarget as HTMLElement
  div.classList.add('line-clamp-none')
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
export function attachRedirectToPageListeners() {
  addEventListenerToElements('[data-redirect-to]', 'click', (e: Event) => {
    e.stopPropagation()
    window.location.href = assertNotNull(
      (e.currentTarget as HTMLElement).dataset.redirectTo,
    )
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
 * Except when the data-override-disable-submit-on-enter attribute is present for the form, the default form action would work
 * See https://github.com/civiform/civiform/issues/5464
 */
function disableEnterToSubmitBehaviorOnForms() {
  addEventListenerToElements('form', 'keydown', (e: KeyboardEvent) => {
    const target = (e.target as HTMLElement).tagName.toLowerCase()
    const overrideDisableSubmitOnEnter = document
      .getElementById((e.target as HTMLElement).id)
      ?.closest('form')
      ?.hasAttribute('data-override-disable-submit-on-enter')
    // If event originated from a button, link, or textarea, or if overrideDisableSubmitOnEnter is enabled,
    // it should proceed with the default action.
    if (
      overrideDisableSubmitOnEnter === false &&
      target !== 'button' &&
      target !== 'a' &&
      target !== 'textarea' &&
      e.key === 'Enter'
    ) {
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

  // Note that this formatting logic mimics QuestionDefinition.getQuestionNameKey()
  const formatQuestionName = (unformatted: string) => {
    const formatted = unformatted
      .toLowerCase()
      .replace(/[^a-zA-Z ]/g, '')
      .replace(/\s/g, '_')
    return formatted
  }

  // Give a live preview of how the question name will be formatted in exports
  const questionNameInput = document.getElementById('question-name-input')
  const formattedOutput: HTMLElement | null =
    document.getElementById('formatted-name')
  if (questionNameInput && formattedOutput) {
    formattedOutput.innerText = formatQuestionName(
      (questionNameInput as HTMLInputElement).value,
    )
    questionNameInput.addEventListener('input', (event: Event) => {
      const target = event.target as HTMLInputElement
      if (formattedOutput && target) {
        formattedOutput.innerText = formatQuestionName(target.value)
      }
    })
  }

  // Bind click handler for remove options in multi-option edit view
  addEventListenerToElements(
    '.multi-option-question-field-remove-button',
    'click',
    removeInput,
  )

  // Bind click handler for re-arranging options in multi-option edit view
  addEventListenerToElements(
    '.multi-option-question-field-move-up-button',
    'click',
    moveMultiOptionQuestionUp,
  )
  addEventListenerToElements(
    '.multi-option-question-field-move-down-button',
    'click',
    moveMultiOptionQuestionDown,
  )

  /* Bind click handler to submit category filter form when any category is clicked
    and set the URL fragment to the most recently clicked category */
  addEventListenerToElements('[id*="check-category"]', 'click', (event) => {
    const form = document.getElementById(
      'category-filter-form',
    ) as HTMLFormElement

    // Update the form action URL with the current selected category as a fragment
    if (event.srcElement) {
      const srcElement = event.srcElement as HTMLElement
      const action = form.action + '#' + srcElement.id
      form.action = action
    }

    form.submit()
  })

  /* Uncheck all program filter checkboxes when the clear filters button is clicked */
  addEventListenerToElements('#clear-filters', 'click', () => {
    const checkboxes = document.querySelectorAll('[id*="ns-check-category"]')

    checkboxes.forEach((checkbox) => {
      const checkboxInput = checkbox as HTMLInputElement
      checkboxInput.checked = false
    })
  })

  attachFormDebouncers()

  attachRedirectToPageListeners()
  attachStopPropogationListenerOnFormButtons()
  disableEnterToSubmitBehaviorOnForms()

  // Advertise (e.g., for browser tests) that main.ts initialization is done
  document.body.dataset.loadMain = 'true'

  // When the user ends their session, clear out local storage, then redirect to the
  // href of the end session button.
  const link = document.querySelector('#logout-button') as HTMLAnchorElement
  if (link) {
    link.addEventListener('click', (event) => {
      event.preventDefault()
      // If we've dismissed the staging warning message, keep it dismissed for the next session
      // for the convenience of developers and cleanliness of screenshots.
      const keepWarningMessage =
        localStorage.getItem('warning-message-dismissed') !== null
      localStorage.clear()
      if (keepWarningMessage) {
        localStorage.setItem('warning-message-dismissed', 'true')
      }
      localStorage.setItem('session_just_ended', 'true')
      window.location.href = link.href
    })
  }
}
