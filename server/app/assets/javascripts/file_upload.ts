import {addEventListenerToElements} from '@/util'
import {isFileTooLarge} from '@/file_upload_util'
import {featureFlags} from '@/global/shared/feature_flags'
import {HtmxRequest} from '@/htmx_request'

const UPLOADED_FILE_ATTR = 'data-uploaded-files'
// Matches a file name with a number "-<number>" at the end. For example "file-2.png"
// Groups are: [1] The file name [2] The "-<number>" [3] - The file type, if it exists (e.g. .png), null otherwise.
const FILE_NAME_DIGIT_SUFFIX_REGEX = /(.*)(-\d*)(\..*)?$/
// Matches a file name with a file type at the end.
// Groups are [1] The file name [2] The file type.
const FILE_NAME_REGEX = /(.*)(\..*)$/
const CF_FILE_UPLOADING_CLASS = 'cf-file-uploading'
const CF_FILE_UPLOAD_QUESTION_SELECTOR = '.cf-question-fileupload'
import {default as uswdsFileInput} from '@uswds/uswds/js/usa-file-input'

// Track the number of file uploads in progress to prevent navigating away
let fileUploadsInProgress = 0

export function init() {
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

  if (featureFlags().isFileUploadQuestionImprovementsEnabled) {
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
        resetFileInput(event)
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
  } else {
    blockForm.addEventListener('change', (event) => {
      const fileInput = event.target as HTMLInputElement
      if (!fileInput || fileInput.type !== 'file') return
      const questionDiv = fileInput.closest(CF_FILE_UPLOAD_QUESTION_SELECTOR)
      if (!questionDiv) return

      if (validateFileUploadQuestion(fileInput)) {
        const elementsToDisable = document.querySelectorAll(
          '.cf-disable-when-uploading',
        )
        elementsToDisable.forEach((elementToDisable) => {
          elementToDisable.setAttribute('disabled', '')
          elementToDisable.setAttribute('aria-disabled', 'true')
          elementToDisable.setAttribute('href', '#')
        })
        if (!featureFlags().isFileUploadQuestionImprovementsEnabled) {
          document.body.classList.add(CF_FILE_UPLOADING_CLASS)
          blockForm.submit()
        }
      }
    })

    const uploadedFilesAttribute = blockForm
      .querySelector(`[${UPLOADED_FILE_ATTR}]`)
      ?.getAttribute(UPLOADED_FILE_ATTR)

    if (uploadedFilesAttribute) {
      const uploadedFilesArray = JSON.parse(uploadedFilesAttribute) as string[]
      blockForm.addEventListener('formdata', (event) => {
        const formData = event.formData
        const file = formData.get('file') as File

        const newName = getUniqueName(file.name, uploadedFilesArray)
        if (file.name != newName) {
          // Rename uploaded file, if a file with the same name has already been uploaded.
          formData.delete('file')
          formData.append('file', file, newName)
        }
      })
    }
  }
}

/**
 * Returns a unique name.
 *
 * Note: This is only exported so we can test it. It should be considered private.
 *
 * @param name The name of the file which must be unique.
 * @param existingNames Array of existing names.
 * @returns unique name, based on the input name, which doesn't match any of the existing names. It does
 * this by appending a "-2", before the file type, or if a number already exists, a -"n+1".
 */
export function getUniqueName(name: string, existingNames: string[]) {
  while (existingNames.includes(name)) {
    const fileNameWithDigitSuffixMatch = name.match(
      FILE_NAME_DIGIT_SUFFIX_REGEX,
    )
    let numberToAppend = 2
    if (fileNameWithDigitSuffixMatch) {
      // File name already has a digit suffix. Increment that instead.
      numberToAppend =
        parseInt(fileNameWithDigitSuffixMatch[2].substring(1)) + 1
      // Put number before the file type ('.png') suffix if the file type is present ([3] is not null)
      name =
        fileNameWithDigitSuffixMatch[1] +
        '-' +
        numberToAppend +
        (fileNameWithDigitSuffixMatch[3] ?? '')
    } else {
      const fileNameWithoutDigitMatch = name.match(FILE_NAME_REGEX)
      if (fileNameWithoutDigitMatch) {
        // Put number before the file type ('.png').
        name =
          fileNameWithoutDigitMatch[1] +
          '-' +
          numberToAppend +
          fileNameWithoutDigitMatch[2]
      } else {
        // Otherwise, just add it to the end.
        name += '-' + numberToAppend
      }
    }
  }

  return name
}

/**
 * Validates the file upload question, showing an error if no file has been uploaded
 * and hiding the error otherwise.
 *
 * @returns true if a file was uploaded and false otherwise.
 */
function validateFileUploadQuestion(fileInput: HTMLInputElement): boolean {
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

  // A valid file upload question is one that has an uploaded file that isn't too large.
  return isFileUploaded && !isFileTooLargeResult
}

/** Shows the error in the specified {@code errorDiv}. */
function showError(errorDiv: HTMLElement | null, fileInput: HTMLInputElement) {
  if (errorDiv == null) {
    return
  }

  errorDiv.hidden = false
  fileInput.setAttribute('aria-invalid', 'true')

  const errorId = errorDiv.getAttribute('id')
  if (!errorId) {
    return
  }

  const ariaDescribedBy = fileInput.getAttribute('aria-describedby') ?? ''
  if (!ariaDescribedBy.includes(errorId)) {
    fileInput.setAttribute('aria-describedby', `${errorId} ${ariaDescribedBy}`)
  }
}

/** Hides the error in the specified {@code errorDiv}. */
function hideError(errorDiv: HTMLElement | null, fileInput: HTMLInputElement) {
  if (errorDiv == null) {
    return
  }

  errorDiv.hidden = true
  fileInput.removeAttribute('aria-invalid')

  const errorId = errorDiv.getAttribute('id')
  if (!errorId) {
    return
  }

  const ariaDescribedBy = fileInput.getAttribute('aria-describedby') ?? ''
  if (ariaDescribedBy.includes(errorId)) {
    const ariaDescribedByWithoutError = ariaDescribedBy.replace(errorId, '')
    fileInput.setAttribute('aria-describedby', ariaDescribedByWithoutError)
  }
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
