import {hideError, isFileTooLarge, showError} from '@/file_upload_util'

// Keep in sync with server/app/views/admin/programs/ProgramImageFragment.html
const PROGRAM_IMAGE_FILE_UPLOAD_ID = 'program-image-input'
const ALT_TEXT_INPUT_ID = 'summaryImageDescription'
const PROGRAM_IMAGE_FORM_ID = 'program-image-form'
// Keep in sync with cf:input error span id for name="summaryImageDescription".
const ALT_REQUIRED_ERROR_ID = 'error-message-summaryImageDescription'
const SAVE_BUTTON_ID = 'save-button'

export const init = () => {
  if (!document.getElementById(PROGRAM_IMAGE_FILE_UPLOAD_ID)) {
    return
  }

  const form = document.getElementById(PROGRAM_IMAGE_FORM_ID)
  const altInput = document.getElementById(ALT_TEXT_INPUT_ID)
  const fileInput = document.getElementById(PROGRAM_IMAGE_FILE_UPLOAD_ID)
  const saveButton = document.getElementById(SAVE_BUTTON_ID)

  if (
    !(form instanceof HTMLFormElement) ||
    !(altInput instanceof HTMLInputElement) ||
    !(fileInput instanceof HTMLInputElement) ||
    !(saveButton instanceof HTMLButtonElement)
  ) {
    return
  }

  altInput.addEventListener('input', () => {
    validateAltTextField(altInput)
    syncAltTextRequiredState(altInput, fileInput)
    syncSaveButtonState(altInput, fileInput, saveButton)
  })

  fileInput.addEventListener('change', () => {
    validateFileInput(fileInput)
    syncAltTextRequiredState(altInput, fileInput)
    syncSaveButtonState(altInput, fileInput, saveButton)
  })

  form.addEventListener('submit', (event) => {
    if (
      (fileInput.value !== '' && !validateFileInput(fileInput)) ||
      !validateAltTextField(altInput)
    ) {
      event.preventDefault()
    }
  })

  syncAltTextRequiredState(altInput, fileInput)
  syncSaveButtonState(altInput, fileInput, saveButton)
}

const syncAltTextRequiredState = (
  altInput: HTMLInputElement,
  fileInput: HTMLInputElement,
) => {
  if (
    fileInput.value !== '' ||
    fileInput.getAttribute('data-has-existing-image') === 'true'
  ) {
    altInput.required = true
    altInput.setAttribute('aria-required', 'true')
  } else {
    altInput.required = false
    altInput.removeAttribute('aria-required')
  }
}

const syncSaveButtonState = (
  altInput: HTMLInputElement,
  fileInput: HTMLInputElement,
  saveButton: HTMLButtonElement,
) => {
  const descriptionChanged = altInput.value !== altInput.defaultValue
  const hasValidNewFile = fileInput.value !== '' && !isFileTooLarge(fileInput)
  saveButton.disabled = !(descriptionChanged || hasValidNewFile)
}

/**
 * Validates the program image file input: toggles the too-large alert and, when valid, clears
 * related error UI.
 *
 * @returns true when a file is selected and its size is within the configured limit.
 */
const validateFileInput = (fileInput: HTMLInputElement): boolean => {
  const container = fileInput.closest<HTMLElement>('.usa-form-group')
  if (!container) {
    return false
  }

  const fileTooLargeErrorDiv = container.querySelector<HTMLElement>(
    '[data-fileupload-error="too-large"]',
  )

  const isFileTooLargeResult = isFileTooLarge(fileInput)
  if (isFileTooLargeResult) {
    showError(fileTooLargeErrorDiv, fileInput)
  } else {
    hideError(fileTooLargeErrorDiv, fileInput)
  }

  const isValid = fileInput.value !== '' && !isFileTooLargeResult
  if (isValid) {
    container
      .querySelectorAll<HTMLElement>('.cf-question-error-message')
      .forEach((el) => {
        el.hidden = true
      })
  }
  return isValid
}

const validateAltTextField = (altInput: HTMLInputElement): boolean => {
  const errorDiv = document.getElementById(ALT_REQUIRED_ERROR_ID)
  const formGroup = altInput.closest('.usa-form-group')
  const valid = !altInput.required || altInput.value.trim() !== ''
  if (valid) {
    hideError(errorDiv, altInput)
    formGroup?.classList.remove('usa-form-group--error')
  } else {
    showError(errorDiv, altInput)
    formGroup?.classList.add('usa-form-group--error')
  }
  return valid
}
