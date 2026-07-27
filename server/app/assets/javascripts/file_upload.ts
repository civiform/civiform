import {hideError, isFileTooLarge, showError} from '@/file_upload_util'
import {default as uswdsFileInput} from '@uswds/uswds/js/usa-file-input'
import {HtmxAfterRequestEvent} from '@/types/htmx'

const CAN_UPLOAD_FILE_ATTR = 'data-can-upload-file'
// Shows the question's "Uploading…" badge on the question container during upload
const CF_FILE_UPLOADING_CLASS = 'cf-file-uploading'
// Disables nav and all .cf-disable-when-uploading elements
const CF_FILE_UPLOAD_IN_PROGRESS_CLASS = 'cf-file-upload-in-progress'
const CF_FILE_UPLOAD_CONTAINER_SELECTOR = '[data-cf-file-upload-container]'
const FILE_UPLOAD_HTMX_FAILURE = '[data-fileupload-error="request-failed"]'
// Attribute on the file list element containing a JSON array of uploaded file names
const UPLOADED_FILES_ATTR = 'data-uploaded-files'
// Attribute on the upload container containing the i18n label template for when files are uploaded.
// Contains a {0} placeholder replaced client-side with the file count.
const FILES_UPLOADED_ARIA_LABEL_ATTR = 'data-files-uploaded-aria-label'

// Track the number of file uploads in progress to prevent navigating away
let fileUploadsInProgress = 0

export const init = () => {
  if (!document.querySelector(CF_FILE_UPLOAD_CONTAINER_SELECTOR)) {
    return
  }

  // Sync aria-labels on initial page load (e.g. if the user navigated back to a
  // page with already-uploaded files).
  syncFileInputAriaLabel()

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
    if (fileUploadContainer) {
      hideError(
        fileUploadContainer.querySelector<HTMLElement>(
          FILE_UPLOAD_HTMX_FAILURE,
        ),
        fileInput,
      )
      fileUploadContainer.classList.add(CF_FILE_UPLOADING_CLASS)
    }

    fileUploadsInProgress++
    document.body.classList.add(CF_FILE_UPLOAD_IN_PROGRESS_CLASS)
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
      document.body.classList.remove(CF_FILE_UPLOAD_IN_PROGRESS_CLASS)
    }
    if (fileUploadContainer) {
      fileUploadContainer.classList.remove(CF_FILE_UPLOADING_CLASS)
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

  document.body.addEventListener('htmx:afterSwap', () => {
    syncFileInputDisabledState()
    syncFileInputAriaLabel()
    toggleDisabledState()
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
}

const syncFileInputDisabledState = () => {
  document
    .querySelectorAll<HTMLElement>(CF_FILE_UPLOAD_CONTAINER_SELECTOR)
    .forEach((container) => {
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
    })
}

/**
 * Updates the aria-label of each file input to reflect the number of already-uploaded
 * files. When files have been uploaded, the file input shows "No file selected" to
 * screen readers (because the input value is cleared after upload), which is confusing.
 * Setting an aria-label with the uploaded file count provides screen reader users with
 * the correct context about already-uploaded files.
 */
const syncFileInputAriaLabel = () => {
  document
    .querySelectorAll<HTMLElement>(CF_FILE_UPLOAD_CONTAINER_SELECTOR)
    .forEach((container) => {
      const fileList = container.querySelector<HTMLElement>(
        `[${UPLOADED_FILES_ATTR}]`,
      )
      const fileInput =
        container.querySelector<HTMLInputElement>('input[type=file]')
      if (!fileList || !fileInput) return

      const uploadedFilesAttr = fileList.getAttribute(UPLOADED_FILES_ATTR)
      if (!uploadedFilesAttr) return

      let uploadedFiles: string[] = []
      try {
        uploadedFiles = JSON.parse(uploadedFilesAttr) as string[]
      } catch {
        return
      }

      if (uploadedFiles.length > 0) {
        const labelTemplate =
          container.getAttribute(FILES_UPLOADED_ARIA_LABEL_ATTR) ?? ''
        const label = labelTemplate.replace('%count%', String(uploadedFiles.length))
        fileInput.setAttribute('aria-label', label)
      } else {
        // No files uploaded — remove any previously set aria-label so the
        // browser's native "No file selected" text is used.
        fileInput.removeAttribute('aria-label')
      }
    })
}
