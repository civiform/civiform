import {addEventListenerToElements, assertNotNull} from './util'
import {isFileTooLarge} from './file_upload_util'

const UPLOAD_ATTR = 'data-upload-text'
const UPLOADED_FILE_ATTR = 'data-uploaded-files'
// Matches a file name with a number "-<number>" at the end. For example "file-2.png"
// Groups are: [1] The file name [2] The "-<number>" [3] - The file type, if it exists (e.g. .png), null otherwise.
const FILE_NAME_DIGIT_SUFFIX_REGEX = /(.*)(-\d*)(\..*)?$/
// Matches a file name with a file type at the end.
// Groups are [1] The file name [2] The file type.
const FILE_NAME_REGEX = /(.*)(\..*)$/

export function init() {
  // Don't add extra logic if we don't have a block form with a
  // file upload question.
  const blockForm = document.getElementById('cf-block-form') as HTMLFormElement
  if (!blockForm) {
    return
  }
  const fileUploadQuestion = blockForm.querySelector('.cf-question-fileupload')
  if (!fileUploadQuestion) {
    // If there's no file upload question on the page, don't add extra logic.
    return
  }

  addEventListenerToElements(
    '.file-upload-action-button',
    'click',
    (e: Event) => {
      onActionButtonClicked(e, blockForm)
    },
  )

  blockForm.addEventListener('submit', (event) => {
    // Prevent submission of a file upload form if no file has been
    // selected. Note: For optional file uploads, a distinct skip button
    // is shown.
    if (!validateFileUploadQuestion(blockForm)) {
      event.preventDefault()
      return false
    }
    return true
  })

  const uploadedDivs = blockForm.querySelectorAll(`[${UPLOAD_ATTR}]`)

  blockForm.addEventListener('change', (event) => {
    const files = (event.target! as HTMLInputElement).files
    const file = assertNotNull(files)[0]
    if (uploadedDivs.length) {
      const uploadedDiv: HTMLDivElement = uploadedDivs[0] as HTMLDivElement
      const uploadText = assertNotNull(uploadedDiv.getAttribute(UPLOAD_ATTR))
      uploadedDiv.innerText = uploadText.replace('{0}', file.name)
    }

    // If we don't have the div showing the latest file upload (from the older single-file upload
    // behavior), then multiple file upload feature is enabled, in that case, submit the form
    // as soon as the applicant selects a file so it immediately uploads the file.
    if (validateFileUploadQuestion(blockForm) && !uploadedDivs.length) {
      const elementsToDisable = document.querySelectorAll(
        '.cf-disable-when-uploading',
      )
      elementsToDisable.forEach((elementToDisable) => {
        elementToDisable.setAttribute('disabled', '')
        elementToDisable.setAttribute('aria-disabled', 'true')
        elementToDisable.setAttribute('href', '#')
      })
      document.body.classList.add('cf-file-uploading')
      blockForm.submit()
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

function onActionButtonClicked(e: Event, blockForm: Element) {
  const buttonTarget = e.currentTarget as HTMLElement
  const fileInput = assertNotNull(
    blockForm.querySelector<HTMLInputElement>('input[type=file]'),
  )

  if (fileInput.value != '') {
    modifySuccessActionRedirect(buttonTarget, blockForm)
    return
  }

  const redirectWithoutFile = buttonTarget.dataset.redirectWithoutFile
  if (redirectWithoutFile) {
    // If there's no file uploaded but the button provides a redirect
    // that can be used even when there's no file, invoke that redirect.
    // See {@link views.applicant.ApplicantFileUploadRenderer.java}.
    window.location.href = redirectWithoutFile
    // This will prevent form submission, which is important because we
    // don't want to send an empty file to cloud storage providers or
    // store an empty file key in our database.
    e.preventDefault()
  }
}

/**
 * Modifies the "success_action_redirect"-named <input> to have the correct redirect
 * location based on the button that was clicked.next
 *
 * Context: When a user submits a file upload, the <form> data is first sent to the
 * cloud storage provider (CSP) to store the file. Once the file is successfully uploaded,
 * the CSP invokes the URL specified by the "success_action_redirect" input to redirect
 * the user appropriately. The "Save&next", "Previous", and "Review" buttons should
 * all upload the file to the CSP, but should redirect to different places after the
 * upload is successful. Since there's only one "success_action_redirect" input in
 * the form, we need to manually edit that input to redirect to the right place.
 *
 * Each button stores a 'redirectWithFile' key in their data that specifies the correct
 * redirect, so this function modifies the "success_action_redirect" input to use the
 * redirect stored in the button. See {@link views.applicant.ApplicantFileUploadRenderer.java}.
 */
function modifySuccessActionRedirect(
  buttonTarget: HTMLElement,
  blockForm: Element,
) {
  const redirectWithFile = assertNotNull(buttonTarget.dataset.redirectWithFile)
  // Note: success_action_redirect is AWS-specific. We'll need to
  // handle Azure differently if/when we decide to support it.
  const successActionRedirectInput = assertNotNull(
    blockForm.querySelector<HTMLInputElement>(
      'input[name="success_action_redirect"]',
    ),
  )
  successActionRedirectInput.value = redirectWithFile
}

/**
 * Validates the file upload question, showing an error if no file has been uploaded
 * and hiding the error otherwise.
 *
 * @returns true if a file was uploaded and false otherwise.
 */
function validateFileUploadQuestion(blockForm: Element): boolean {
  // Note: Currently, a file upload question must be on a screen by itself with no
  // other questions (file upload or otherwise). This method implementation assumes
  // that there is a single question on the page. If we later allow file upload
  // questions to be with other questions, we'll need to update this method.
  const fileInput = assertNotNull(
    blockForm.querySelector<HTMLInputElement>('input[type=file]'),
  )
  const isFileUploaded = fileInput.value != ''

  const fileNotSelectedErrorDiv = document.getElementById(
    'cf-fileupload-required-error',
  ) as HTMLElement
  if (!isFileUploaded) {
    showError(fileNotSelectedErrorDiv, fileInput)
  } else {
    hideError(fileNotSelectedErrorDiv, fileInput)
  }

  const isFileTooLargeResult = isFileTooLarge(fileInput)
  const fileTooLargeErrorDiv = document.getElementById(
    'cf-fileupload-too-large-error',
  ) as HTMLElement
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
