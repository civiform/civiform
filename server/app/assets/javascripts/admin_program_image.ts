import {hideError, isFileTooLarge, showError} from '@/file_upload_util'

// Keep in sync with server/app/views/admin/programs/ProgramImageFragment.html
const PROGRAM_IMAGE_FILE_UPLOAD_ID = 'program-image-input'
const ALT_TEXT_INPUT_ID = 'summaryImageDescription'
const PROGRAM_IMAGE_FORM_ID = 'program-image-form'
const ALT_REQUIRED_ERROR_ID = 'cf-program-image-alt-required-error'

export const init = () => {
  if (!document.getElementById(PROGRAM_IMAGE_FILE_UPLOAD_ID)) {
    return
  }

  const form = document.getElementById(PROGRAM_IMAGE_FORM_ID)
  const altInput = document.getElementById(ALT_TEXT_INPUT_ID)
  const fileInput = document.getElementById(PROGRAM_IMAGE_FILE_UPLOAD_ID)

  if (
    !(form instanceof HTMLFormElement) ||
    !(altInput instanceof HTMLInputElement) ||
    !(fileInput instanceof HTMLInputElement)
  ) {
    return
  }

  altInput.addEventListener('input', () => {
    validateAltTextField(altInput)
  })

  fileInput.addEventListener('change', () => {
    validateFileInput(fileInput)
    syncAltTextRequiredState(altInput, fileInput)
  })

  form.addEventListener('submit', (event) => {
    if (!validateFormSubmit(altInput, fileInput)) {
      event.preventDefault()
    }
  })

  syncAltTextRequiredState(altInput, fileInput)
}

const syncAltTextRequiredState = (
  altInput: HTMLInputElement,
  fileInput: HTMLInputElement,
) => {
  const isRequired = fileInput.value !== ''
  altInput.required = isRequired
  if (isRequired) {
    altInput.setAttribute('aria-required', 'true')
  } else {
    altInput.removeAttribute('aria-required')
  }
}

const isAltTextValid = (altInput: HTMLInputElement): boolean =>
  !altInput.required || altInput.value.trim() !== ''

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

const validateFormSubmit = (
  altInput: HTMLInputElement,
  fileInput: HTMLInputElement,
): boolean => {
  if (fileInput.value !== '' && !validateFileInput(fileInput)) {
    return false
  }
  return validateAltTextField(altInput)
}

const validateAltTextField = (altInput: HTMLInputElement): boolean => {
  const errorDiv = document.getElementById(ALT_REQUIRED_ERROR_ID)
  const valid = isAltTextValid(altInput)
  if (valid) {
    hideError(errorDiv, altInput)
  } else {
    showError(errorDiv, altInput)
  }
  return valid
}
