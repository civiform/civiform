import {addEventListenerToElements} from '@/util'
import {
  getUniqueName,
  hideError,
  isFileTooLarge,
  showError,
} from '@/file_upload_util'
import {HtmxRequest} from '@/htmx_request'
import {default as uswdsFileInput} from '@uswds/uswds/js/usa-file-input'

const UPLOADED_FILE_ATTR = 'data-uploaded-files'
const CF_FILE_UPLOADING_CLASS = 'cf-file-uploading'
const CF_FILE_UPLOAD_QUESTION_SELECTOR = '.cf-question-fileupload'

// Track the number of file uploads in progress to prevent navigating away
let fileUploadsInProgress = 0

export const init = () => {
  // Don't add extra logic if we don't have a block form with a
  // file upload question.
  const blockForm = document.getElementById('cf-block-form') as HTMLFormElement
  if (!blockForm) {
    return
  }
  const fileUploadQuestion = blockForm.querySelector(
    CF_FILE_UPLOAD_QUESTION_SELECTOR,
  )
  if (!fileUploadQuestion) {
    // If there's no file upload question on the page, don't add extra logic.
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
    if (!isFileUploadHtmxEvent(event)) return
    const fileInput = event.target as HTMLInputElement

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

  document.body.addEventListener('htmx:afterOnLoad', (event) => {
    if (!isFileUploadHtmxEvent(event)) return
    fileUploadsInProgress--
    if (fileUploadsInProgress <= 0) {
      fileUploadsInProgress = 0
      document.body.classList.remove(CF_FILE_UPLOADING_CLASS)
    }
    toggleDisabledState()
    resetFileInput(event)
  })

  document.body.addEventListener('htmx:responseError', (event) => {
    if (!isFileUploadHtmxEvent(event)) return
    fileUploadsInProgress--
    if (fileUploadsInProgress <= 0) {
      fileUploadsInProgress = 0
      document.body.classList.remove(CF_FILE_UPLOADING_CLASS)
    }
    toggleDisabledState()
  })

  document.body.addEventListener('htmx:configRequest', (event) => {
    if (!isFileUploadHtmxEvent(event)) return
    const customEvent = event as CustomEvent<HtmxRequest>
    const triggerElt = customEvent.detail.elt

    const questionDiv = triggerElt.closest(CF_FILE_UPLOAD_QUESTION_SELECTOR)
    if (!questionDiv) return

    const uploadedFilesAttribute = questionDiv
      .querySelector(`[${UPLOADED_FILE_ATTR}]`)
      ?.getAttribute(UPLOADED_FILE_ATTR)

    if (!uploadedFilesAttribute) return
    const uploadedFilesArray = JSON.parse(uploadedFilesAttribute) as string[]

    const formData = customEvent.detail.formData
    const file = formData.get('file')
    if (!(file instanceof File)) return

    const newName = getUniqueName(file.name, uploadedFilesArray)

    if (file.name !== newName) {
      formData.delete('file')
      formData.append('file', file, newName)
    }
  })

  addEventListenerToElements('#cf-block-form', 'change', (event) => {
    const fileInput = event.target as HTMLInputElement
    validateFileUploadQuestion(fileInput)
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
  const questionDiv = fileInput.closest(CF_FILE_UPLOAD_QUESTION_SELECTOR)
  if (!questionDiv) return false

  const isFileUploaded = fileInput.value !== ''

  const fileNotSelectedErrorDiv = questionDiv.querySelector<HTMLElement>(
    '[data-fileupload-error="required"]',
  )
  if (!isFileUploaded) {
    showError(fileNotSelectedErrorDiv, fileInput)
  } else {
    hideError(fileNotSelectedErrorDiv, fileInput)
  }

  const isFileTooLargeResult = isFileTooLarge(fileInput)
  const fileTooLargeErrorDiv = questionDiv.querySelector<HTMLElement>(
    '[data-fileupload-error="too-large"]',
  )

  if (isFileTooLargeResult) {
    showError(fileTooLargeErrorDiv, fileInput)
  } else {
    hideError(fileTooLargeErrorDiv, fileInput)
  }

  const isValid = isFileUploaded && !isFileTooLargeResult
  if (isValid) {
    questionDiv
      .querySelectorAll<HTMLElement>('.cf-question-error-message')
      .forEach((el) => (el.hidden = true))
  }
  // A valid file upload question is one that has an uploaded file that isn't too large.
  return isValid
}

const isFileUploadHtmxEvent = (event: Event) => {
  const detail = (event as CustomEvent).detail as
    | {elt?: HTMLElement}
    | undefined
  return detail?.elt?.closest(CF_FILE_UPLOAD_QUESTION_SELECTOR) != null
}

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

const resetFileInput = (event: Event) => {
  const detail = (event as CustomEvent).detail as
    | {elt?: HTMLElement}
    | undefined
  const questionDiv = detail?.elt?.closest(
    CF_FILE_UPLOAD_QUESTION_SELECTOR,
  ) as HTMLElement
  if (!questionDiv) return

  const fileInput =
    questionDiv.querySelector<HTMLInputElement>('input[type=file]')
  if (fileInput) {
    fileInput.value = ''
    uswdsFileInput.off(questionDiv)
    uswdsFileInput.on(questionDiv)
  }
}
