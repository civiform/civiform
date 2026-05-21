import {hideError, isFileTooLarge, showError} from '@/file_upload_util'
import {default as uswdsFileInput} from '@uswds/uswds/js/usa-file-input'
import {HtmxAfterRequestEvent, HtmxAfterSwapEvent} from '@/types/htmx'

const CAN_UPLOAD_FILE_ATTR = 'data-can-upload-file'
const CF_FILE_UPLOADING_CLASS = 'cf-file-uploading'
const CF_FILE_UPLOAD_CONTAINER_SELECTOR = '[data-cf-file-upload-container]'
const FILE_UPLOAD_VALIDATION_ERRORS_SELECTOR =
  '[id^="cf-fileupload-validation-errors-"]'
const USWDS_FILE_INPUT_SR_STATUS_SELECTOR =
  '.usa-file-input > .usa-sr-only[aria-live="polite"]'
const FILE_UPLOAD_HTMX_FAILURE = '[data-fileupload-error="request-failed"]'

// Track the number of file uploads in progress to prevent navigating away
let fileUploadsInProgress = 0
let maxFilesAnnounceOffset = 0
let validationAnnounceOffset = 0
let uploadingAnnounceOffset = 0

const getUploadedFileCount = (container: HTMLElement): number => {
  const fileList = container.querySelector('[id^="cf-fileupload-file-list-"]')
  return fileList ? fileList.querySelectorAll('li').length : 0
}

const getSelectionStatusMessage = (
  container: HTMLElement,
): string | undefined => {
  const statusEl = container.querySelector<HTMLElement>(
    '[data-fileupload-selection-status]',
  )
  if (!statusEl) return undefined

  const fileList = container.querySelector<HTMLElement>(
    '[id^="cf-fileupload-file-list-"]',
  )
  if (fileList?.getAttribute(CAN_UPLOAD_FILE_ATTR) === 'false') {
    return container.querySelector<HTMLElement>(
      '[data-fileupload-max-files-status]',
    )?.dataset.message
  }

  const fileCount = getUploadedFileCount(container)
  if (fileCount === 0) return statusEl.dataset.messageNone
  if (fileCount === 1) return statusEl.dataset.messageSingular
  return statusEl.dataset.messagePlural?.replace('{0}', String(fileCount))
}

const announceLiveRegion = (
  el: HTMLElement | null,
  message: string | undefined,
  offset: number,
): number => {
  if (!el || !message) return offset
  let text = message
  if (offset % 2 === 0) text += '.'
  el.textContent = text
  setTimeout(() => {
    el.textContent = ''
  }, 1000)
  return offset + 1
}

const syncFileUploadA11y = (container: HTMLElement) => {
  const fileInput =
    container.querySelector<HTMLInputElement>('input[type=file]')
  if (!fileInput || !container.id) return

  const titleId = `${container.id}-title`
  if (document.getElementById(titleId)) {
    fileInput.removeAttribute('aria-label')
    fileInput.setAttribute('aria-labelledby', titleId)
  }

  container
    .querySelector<HTMLElement>(USWDS_FILE_INPUT_SR_STATUS_SELECTOR)
    ?.setAttribute('aria-hidden', 'true')

  const statusEl = container.querySelector<HTMLElement>(
    '[data-fileupload-selection-status]',
  )
  const statusMessage = getSelectionStatusMessage(container)
  if (statusEl && statusMessage) {
    statusEl.textContent = statusMessage
  }

  const validationEl = container.querySelector<HTMLElement>(
    FILE_UPLOAD_VALIDATION_ERRORS_SELECTOR,
  )
  const errorMessage =
    validationEl
      ?.querySelector<HTMLElement>('.cf-question-error-message')
      ?.textContent?.trim() ?? ''
  const hasErrors = errorMessage !== ''

  if (hasErrors && validationEl) {
    container.classList.add('cf-question-field-with-error')
    fileInput.setAttribute('aria-invalid', 'true')
    validationAnnounceOffset = announceLiveRegion(
      container.querySelector<HTMLElement>(
        '[data-fileupload-validation-status]',
      ),
      errorMessage,
      validationAnnounceOffset,
    )
  } else {
    container.classList.remove('cf-question-field-with-error')
    fileInput.removeAttribute('aria-invalid')
  }
}

export const init = () => {
  if (!document.querySelector(CF_FILE_UPLOAD_CONTAINER_SELECTOR)) {
    return
  }

  document
    .querySelectorAll<HTMLElement>(CF_FILE_UPLOAD_CONTAINER_SELECTOR)
    .forEach(syncFileUploadA11y)

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

    const fileUploadContainer = fileInput.closest(
      CF_FILE_UPLOAD_CONTAINER_SELECTOR,
    )
    if (fileUploadContainer instanceof HTMLElement) {
      hideError(
        fileUploadContainer.querySelector<HTMLElement>(
          FILE_UPLOAD_HTMX_FAILURE,
        ),
        fileInput,
      )
      const uploadingStatus = fileUploadContainer.querySelector<HTMLElement>(
        '[data-fileupload-uploading-status]',
      )
      uploadingAnnounceOffset = announceLiveRegion(
        uploadingStatus,
        uploadingStatus?.dataset.message,
        uploadingAnnounceOffset,
      )
    }

    fileUploadsInProgress++
    document.body.classList.add(CF_FILE_UPLOADING_CLASS)
    toggleDisabledState()
  })

  document.body.addEventListener('htmx:afterRequest', (event) => {
    if (!isCfFileUploadInput(event.detail.elt)) {
      return
    }

    const fileUploadContainer = event.detail.elt.closest(
      CF_FILE_UPLOAD_CONTAINER_SELECTOR,
    )

    fileUploadsInProgress--
    if (fileUploadsInProgress <= 0) {
      fileUploadsInProgress = 0
      document.body.classList.remove(CF_FILE_UPLOADING_CLASS)
    }
    toggleDisabledState()
    if (event.detail.successful) {
      if (fileUploadContainer) {
        hideError(
          fileUploadContainer.querySelector<HTMLElement>(
            FILE_UPLOAD_HTMX_FAILURE,
          ),
          event.detail.elt,
        )
      }
      resetFileInput(event)
    } else if (fileUploadContainer && !event.detail.successful) {
      showError(
        fileUploadContainer.querySelector<HTMLElement>(
          FILE_UPLOAD_HTMX_FAILURE,
        ),
        event.detail.elt,
      )
    }
  })

  document.body.addEventListener(
    'htmx:afterSwap',
    (event: HtmxAfterSwapEvent) => {
      const container =
        event.detail.target.closest<HTMLElement>(
          CF_FILE_UPLOAD_CONTAINER_SELECTOR,
        ) ??
        event.detail.elt.closest<HTMLElement>(CF_FILE_UPLOAD_CONTAINER_SELECTOR)
      if (!container) return

      syncFileInputDisabledState(container)
      syncFileUploadA11y(container)
    },
  )

  document.body.addEventListener('change', (event) => {
    if (!isCfFileUploadInput(event.target)) {
      return
    }
    validateFileUploadQuestion(event.target)
    const container = event.target.closest(CF_FILE_UPLOAD_CONTAINER_SELECTOR)
    if (container instanceof HTMLElement) {
      setTimeout(() => syncFileUploadA11y(container), 0)
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
    hideError(
      fileUploadContainer.querySelector<HTMLElement>(FILE_UPLOAD_HTMX_FAILURE),
      fileInput,
    )
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
  }
  uswdsFileInput.off(fileUploadContainer)
  uswdsFileInput.on(fileUploadContainer)
  syncFileUploadA11y(fileUploadContainer)
}

const syncFileInputDisabledState = (container: HTMLElement) => {
  const fileList = container.querySelector(`[${CAN_UPLOAD_FILE_ATTR}]`)
  if (!fileList) return

  const fileInput =
    container.querySelector<HTMLInputElement>('input[type=file]')
  if (!fileInput) return

  const canUpload = fileList.getAttribute(CAN_UPLOAD_FILE_ATTR) === 'true'
  if (canUpload) {
    uswdsFileInput.enable(fileInput)
  } else {
    uswdsFileInput.disable(fileInput)
  }

  if (!canUpload) {
    const maxFilesStatus = container.querySelector<HTMLElement>(
      '[data-fileupload-max-files-status]',
    )
    maxFilesAnnounceOffset = announceLiveRegion(
      maxFilesStatus,
      maxFilesStatus?.dataset.message,
      maxFilesAnnounceOffset,
    )
  }
}
