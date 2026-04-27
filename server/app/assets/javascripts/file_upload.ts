import {
  getUniqueName,
  hideError,
  isFileTooLarge,
  showError,
} from '@/file_upload_util'
import {default as uswdsFileInput} from '@uswds/uswds/js/usa-file-input'
import {HtmxAfterRequestEvent} from '@/types/htmx'

const UPLOADED_FILE_ATTR = 'data-uploaded-files'
const CF_FILE_UPLOADING_CLASS = 'cf-file-uploading'
const CF_FILE_UPLOAD_CONTAINER_SELECTOR = '[data-cf-file-upload-container]'

// Track the number of file uploads in progress to prevent navigating away
let fileUploadsInProgress = 0

export const init = () => {
  if (!document.querySelector(CF_FILE_UPLOAD_CONTAINER_SELECTOR)) {
    return
  }

  window.addEventListener('beforeunload', (e: BeforeUnloadEvent) => {
    if (fileUploadsInProgress > 0) {
      e.preventDefault()
      // Deprecated in favor of preventDefault() but included for legacy browser support
      e.returnValue = true
    }
  })

  document.body.addEventListener('htmx:beforeRequest', (event) => {
    const fileInput = event.detail.elt
    if (!isCfFileUploadInput(fileInput)) {
      return
    }

    // We validate both on the beforeRequest and onchange so that we block the request
    // to the server if the client invalidates the upload
    if (!validateFileUploadQuestion(fileInput)) {
      event.preventDefault()
      return
    }
    fileUploadsInProgress++
    document.body.classList.add(CF_FILE_UPLOADING_CLASS)
    toggleDisabledState()
  })

  document.body.addEventListener('htmx:afterRequest', (event) => {
    if (!isCfFileUploadInput(event.detail.elt)) {
      return
    }
    fileUploadsInProgress--
    if (fileUploadsInProgress <= 0) {
      fileUploadsInProgress = 0
      document.body.classList.remove(CF_FILE_UPLOADING_CLASS)
    }
    toggleDisabledState()
    if (event.detail.successful) {
      resetFileInput(event)
    }
  })

  document.body.addEventListener('htmx:configRequest', (event) => {
    const triggerElt = event.detail.elt
    if (!isCfFileUploadInput(triggerElt)) {
      return
    }

    const fileUploadContainer = triggerElt.closest(
      CF_FILE_UPLOAD_CONTAINER_SELECTOR,
    )
    if (!fileUploadContainer) return

    const uploadedFilesAttribute = fileUploadContainer
      .querySelector(`[${UPLOADED_FILE_ATTR}]`)
      ?.getAttribute(UPLOADED_FILE_ATTR)

    if (!uploadedFilesAttribute) return
    const uploadedFilesArray = JSON.parse(uploadedFilesAttribute) as string[]

    const formData = event.detail.formData
    const file = formData.get('file')
    if (!(file instanceof File)) return

    const newName = getUniqueName(file.name, uploadedFilesArray)

    if (file.name !== newName) {
      formData.delete('file')
      formData.append('file', file, newName)
    }
  })

  document.body.addEventListener('change', (event) => {
    if (isCfFileUploadInput(event.target)) {
      validateFileUploadQuestion(event.target)
    }
  })
}

/**
 * Validates the file upload question, showing an error if no file has been uploaded
 * and hiding the error otherwise.
 *
 * @returns true if a file was uploaded and false otherwise.
 */
const validateFileUploadQuestion = (fileInput: HTMLInputElement): boolean => {
  if (!fileInput || fileInput.type !== 'file') return false
  const fileUploadContainer = fileInput.closest(
    CF_FILE_UPLOAD_CONTAINER_SELECTOR,
  )
  if (!fileUploadContainer) return false

  const isFileUploaded = fileInput.value !== ''

  const fileNotSelectedErrorDiv =
    fileUploadContainer.querySelector<HTMLElement>(
      '[data-fileupload-error="required"]',
    )
  if (!isFileUploaded) {
    showError(fileNotSelectedErrorDiv, fileInput)
  } else {
    hideError(fileNotSelectedErrorDiv, fileInput)
  }

  const isFileTooLargeResult = isFileTooLarge(fileInput)
  const fileTooLargeErrorDiv = fileUploadContainer.querySelector<HTMLElement>(
    '[data-fileupload-error="too-large"]',
  )

  if (isFileTooLargeResult) {
    showError(fileTooLargeErrorDiv, fileInput)
  } else {
    hideError(fileTooLargeErrorDiv, fileInput)
  }

  const isValid = isFileUploaded && !isFileTooLargeResult
  if (isValid) {
    fileUploadContainer
      .querySelectorAll<HTMLElement>('.cf-question-error-message')
      .forEach((el) => (el.hidden = true))
  }
  // A valid file upload question is one that has an uploaded file that isn't too large.
  return isValid
}

const isCfFileUploadInput = (
  elt: EventTarget | null,
): elt is HTMLInputElement =>
  elt instanceof HTMLInputElement && elt.type === 'file'

const toggleDisabledState = () => {
  const elements = document.querySelectorAll('.cf-disable-when-uploading')
  elements.forEach((element) => {
    if (fileUploadsInProgress > 0) {
      element.setAttribute('disabled', '')
      element.setAttribute('aria-disabled', 'true')
    } else {
      element.removeAttribute('disabled')
      element.removeAttribute('aria-disabled')
    }
  })
}

const resetFileInput = (event: HtmxAfterRequestEvent) => {
  const fileUploadContainer = event.detail.elt.closest(
    CF_FILE_UPLOAD_CONTAINER_SELECTOR,
  )
  if (!fileUploadContainer || !(fileUploadContainer instanceof HTMLElement)) {
    return
  }

  const fileInput =
    fileUploadContainer.querySelector<HTMLInputElement>('input[type=file]')
  if (fileInput) {
    fileInput.value = ''
    uswdsFileInput.off(fileUploadContainer)
    uswdsFileInput.on(fileUploadContainer)
  }
}
