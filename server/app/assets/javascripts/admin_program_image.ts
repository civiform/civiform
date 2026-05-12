import type {HtmxAfterRequestEvent, HtmxBeforeRequestEvent} from '@/types/htmx'
import {hideError, isFileTooLarge, showError} from '@/file_upload_util'

// Keep in sync with server/app/views/admin/programs/ProgramImageFragment.html
const PROGRAM_IMAGE_FILE_UPLOAD_ID = 'program-image-input'
const ALT_TEXT_INPUT_ID = 'alt-text'
const SUBMIT_BUTTON_ID = 'program-image-submit-button'
const PROGRAM_IMAGE_FORM_ID = 'program-image-form'
const ALT_REQUIRED_ERROR_ID = 'cf-program-image-alt-required-error'
const CF_FILE_UPLOADING_CLASS = 'cf-file-uploading'

let programImageUploadsInProgress = 0

function isProgramImageFileInput(
  elt: EventTarget | null,
): elt is HTMLInputElement {
  return (
    elt instanceof HTMLInputElement &&
    elt.type === 'file' &&
    elt.id === PROGRAM_IMAGE_FILE_UPLOAD_ID
  )
}

/**
 * Validates the program image file input: toggles the too-large alert and, when valid, clears
 * related error UI. Modeled on {@code validateFileUploadQuestion} in {@code file_upload.ts}.
 *
 * @returns true when a file is selected and its size is within the configured limit.
 */
function validateFileInput(fileInput: HTMLInputElement): boolean {
  if (!fileInput || fileInput.type !== 'file') {
    return false
  }
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

/**
 * Validates alt text: toggles the required-style alert. Listeners only call this and handle
 * {@code preventDefault} on submit when it returns false.
 */
function validateAltText(altInput: HTMLInputElement): boolean {
  const errorDiv = document.getElementById(ALT_REQUIRED_ERROR_ID)
  if (!altInput.required) {
    hideError(errorDiv, altInput)
    return true
  }
  if (altInput.value.trim() === '') {
    showError(errorDiv, altInput)
    return false
  }
  hideError(errorDiv, altInput)
  return true
}

function toggleDisabledState(): void {
  const submit = document.getElementById(SUBMIT_BUTTON_ID)
  if (programImageUploadsInProgress > 0) {
    if (submit instanceof HTMLButtonElement) {
      submit.disabled = true
      submit.setAttribute('aria-disabled', 'true')
    }
  } else {
    if (submit instanceof HTMLButtonElement) {
      submit.disabled = false
      submit.removeAttribute('aria-disabled')
    }
  }
}

/**
 * Disable submit while an HTMX file upload is in flight.
 * After a successful upload, mark alt text required.
 */
export function init(): void {
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

  document.body.addEventListener('change', (event: Event) => {
    if (!isProgramImageFileInput(event.target)) {
      return
    }
    validateFileInput(event.target)
  })

  const form = document.getElementById(PROGRAM_IMAGE_FORM_ID)
  const altInput = document.getElementById(ALT_TEXT_INPUT_ID)
  if (form instanceof HTMLFormElement && altInput instanceof HTMLInputElement) {
    altInput.addEventListener('input', () => {
      validateAltText(altInput)
    })
    form.addEventListener('submit', (event: Event) => {
      if (!validateAltText(altInput)) {
        event.preventDefault()
      }
    })
  }

  document.body.addEventListener('htmx:beforeRequest', (event: Event) => {
    const e = event as HtmxBeforeRequestEvent
    if (!isProgramImageFileInput(e.detail.elt)) {
      return
    }
    if (!validateFileInput(e.detail.elt)) {
      e.preventDefault()
      return
    }
    programImageUploadsInProgress++
    document.body.classList.add(CF_FILE_UPLOADING_CLASS)
    toggleDisabledState()
  })

  document.body.addEventListener('htmx:afterRequest', (event: Event) => {
    const e = event as HtmxAfterRequestEvent
    if (!isProgramImageFileInput(e.detail.elt)) {
      return
    }

    programImageUploadsInProgress--
    if (programImageUploadsInProgress <= 0) {
      programImageUploadsInProgress = 0
      document.body.classList.remove(CF_FILE_UPLOADING_CLASS)
    }
    toggleDisabledState()

    const fileInput = e.detail.elt
    if (e.detail.successful) {
      validateFileInput(fileInput)
    }

    if (!e.detail.successful) {
      return
    }
    const alt = document.getElementById(
      ALT_TEXT_INPUT_ID,
    ) as HTMLInputElement | null
    if (!alt) {
      return
    }
    alt.required = true
    alt.setAttribute('aria-required', 'true')
    alt.disabled = false
    alt.removeAttribute('disabled')
    alt.removeAttribute('aria-disabled')
  })
}
