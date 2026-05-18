import {hideError, isFileTooLarge, showError} from '@/file_upload_util'

// Keep in sync with server/app/views/admin/programs/ProgramImageFragment.html
const PROGRAM_IMAGE_FILE_UPLOAD_ID = 'program-image-input'
const ALT_TEXT_INPUT_ID = 'summaryImageDescription'
const SUBMIT_BUTTON_ID = 'continue-button'
const PROGRAM_IMAGE_FORM_ID = 'image-description-form'
const ALT_REQUIRED_ERROR_ID = 'cf-program-image-alt-required-error'
const CF_FILE_UPLOADING_CLASS = 'cf-file-uploading'

// Track the number of program image uploads in progress to prevent navigating away
let programImageUploadsInProgress = 0

export const init = () => {
  if (!document.getElementById(PROGRAM_IMAGE_FILE_UPLOAD_ID)) {
    return
  }

  window.addEventListener('beforeunload', (e: BeforeUnloadEvent) => {
    if (programImageUploadsInProgress > 0) {
      e.preventDefault()
      // Deprecated in favor of preventDefault() but included for legacy browser support
      e.returnValue = true
    }
  })

  document.body.addEventListener('change', (event) => {
    if (!isProgramImageFileInput(event.target)) {
      return
    }
    const fileInput = event.target
    if (validateFileInput(fileInput)) {
      const alt = document.getElementById(
        ALT_TEXT_INPUT_ID,
      ) as HTMLInputElement | null
      if (alt) {
        enableAltTextField(alt)
      }
      syncContinueButtonState()
    }
  })

  const form = document.getElementById(PROGRAM_IMAGE_FORM_ID)
  const altInput = document.getElementById(ALT_TEXT_INPUT_ID)
  if (form instanceof HTMLFormElement && altInput instanceof HTMLInputElement) {
    altInput.addEventListener('input', () => {
      validateAltTextField(altInput)
    })
    form.addEventListener('submit', (event) => {
      if (!validateAltTextField(altInput)) {
        event.preventDefault()
      }
    })
  }

  syncContinueButtonState()

  document.body.addEventListener('htmx:beforeRequest', (event) => {
    if (!isProgramImageFileInput(event.detail.elt)) {
      return
    }
    if (!validateFileInput(event.detail.elt)) {
      event.preventDefault()
      return
    }
    programImageUploadsInProgress++
    document.body.classList.add(CF_FILE_UPLOADING_CLASS)
    syncContinueButtonState()
  })

  document.body.addEventListener('htmx:afterRequest', (event) => {
    if (!isProgramImageFileInput(event.detail.elt)) {
      return
    }

    programImageUploadsInProgress--
    if (programImageUploadsInProgress <= 0) {
      programImageUploadsInProgress = 0
      document.body.classList.remove(CF_FILE_UPLOADING_CLASS)
    }

    const fileInput = event.detail.elt
    if (event.detail.successful) {
      validateFileInput(fileInput)
      const alt = document.getElementById(
        ALT_TEXT_INPUT_ID,
      ) as HTMLInputElement | null
      if (alt) {
        enableAltTextField(alt)
      }
    }

    syncContinueButtonState()
  })
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

  const isFileUploaded = fileInput.value !== ''

  const fileTooLargeErrorDiv = container.querySelector<HTMLElement>(
    '[data-fileupload-error="too-large"]',
  )

  const isFileTooLargeResult = isFileTooLarge(fileInput)
  if (isFileTooLargeResult) {
    showError(fileTooLargeErrorDiv, fileInput)
  } else {
    hideError(fileTooLargeErrorDiv, fileInput)
  }

  const isValid = isFileUploaded && !isFileTooLargeResult
  if (isValid) {
    container
      .querySelectorAll<HTMLElement>('.cf-question-error-message')
      .forEach((el) => {
        el.hidden = true
      })
  }
  return isValid
}

const isProgramImageFileInput = (
  elt: EventTarget | null,
): elt is HTMLInputElement =>
  elt instanceof HTMLInputElement &&
  elt.type === 'file' &&
  elt.id === PROGRAM_IMAGE_FILE_UPLOAD_ID

const enableAltTextField = (alt: HTMLInputElement) => {
  alt.required = true
  alt.setAttribute('aria-required', 'true')
  alt.disabled = false
  alt.removeAttribute('disabled')
  alt.removeAttribute('aria-disabled')
}

const isAltTextValid = (altInput: HTMLInputElement): boolean =>
  !altInput.required || altInput.value.trim() !== ''

/**
 * Updates continue/save button. The button is disabled during upload or when enabled alt text is invalid.
 */
const syncContinueButtonState = () => {
  const submit = document.getElementById(SUBMIT_BUTTON_ID)
  const altInput = document.getElementById(ALT_TEXT_INPUT_ID)
  if (!(submit instanceof HTMLButtonElement)) {
    return
  }

  if (
    programImageUploadsInProgress > 0 ||
    (altInput instanceof HTMLInputElement && !isAltTextValid(altInput))
  ) {
    submit.disabled = true
    submit.setAttribute('aria-disabled', 'true')
  } else {
    submit.disabled = false
    submit.removeAttribute('aria-disabled')
  }
}

const validateAltTextField = (altInput: HTMLInputElement): boolean => {
  const errorDiv = document.getElementById(ALT_REQUIRED_ERROR_ID)
  const valid = isAltTextValid(altInput)
  if (valid) {
    hideError(errorDiv, altInput)
  } else {
    showError(errorDiv, altInput)
  }
  syncContinueButtonState()
  return valid
}
