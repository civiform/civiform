import {addEventListenerToElements, assertNotNull} from './util'
import {isFileTooLarge} from './file_upload_util'

const UPLOAD_ATTR = 'data-upload-text'

export function init() {
  // Don't add extra logic if we don't have a block form with a
  // file upload question.
  const blockForm = document.getElementById('cf-block-form')
  if (!blockForm) {
    return
  }
  const fileUploadQuestion = blockForm.querySelector('.cf-question-fileupload')
  if (!fileUploadQuestion) {
    // If there's no file upload question on the page, don't add extra logic.
    return

  // This event listener should only trigger if the SAVE_ON_ALL_ACTIONS flag is on
  // because the file-upload-action-button class is only added when that flag is on
  // (see {@link views.applicant.ApplicantFileUploadRenderer.java}).
  addEventListenerToElements(
    '.file-upload-action-button',
    'click',
    (e: Event) => {
      onActionButtonClicked(e, blockForm)
    },

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
  if (uploadedDivs.length) {
    const uploadedDiv = uploadedDivs[0]
    const uploadText = assertNotNull(uploadedDiv.getAttribute(UPLOAD_ATTR))

    blockForm.addEventListener('change', (event) => {
      const files = (event.target! as HTMLInputElement).files
      const file = assertNotNull(files)[0]
      uploadedDiv.innerHTML = uploadText.replace('{0}', file.name)
      validateFileUploadQuestion(blockForm)
    })
  }
}

function onActionButtonClicked(e: Event, blockForm: Element) {
  const buttonTarget = e.currentTarget as HTMLElement
  const fileInput = assertNotNull(
    blockForm.querySelector<HTMLInputElement>('input[type=file]'),
  )

  if (fileInput.value != '') {
    modifySuccessActionRedirect(buttonTarget, blockForm)
  } else {
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

  const errorDiv = blockForm.querySelector(
    '.cf-fileupload-error',
  ) as HTMLElement
  if (!errorDiv) {
    return isFileUploaded
  }

  if (isFileUploaded) {
    hideFileSelectionError(errorDiv, fileInput)
  } else {
    showFileSelectionError(errorDiv, fileInput)
  }
  return isFileUploaded
}

/**
 * Shows a "Please select a file" error. Used when no file was uploaded
 * but the user wants to continue to the next page.
 */
function showFileSelectionError(
  errorDiv: HTMLElement,
  fileInput: HTMLInputElement,
) {
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

/** Hides the "Please select a file" error. */
function hideFileSelectionError(
  errorDiv: HTMLElement,
  fileInput: HTMLInputElement,
) {
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

//     const tooLargeErrorDiv = blockForm.querySelector(
 //        '.cf-fileupload-too-large-error',
 //      )
function checkFileSize(
  fileInput: HTMLInputElement,
  tooLargeErrorDiv: Element | null,
): boolean {
  const fileTooLarge = isFileTooLarge(fileInput)
  if (tooLargeErrorDiv) {
    tooLargeErrorDiv.classList.toggle('hidden', !fileTooLarge)
    if (fileTooLarge) {
      // Add ariaLive label so error is announced to screen reader.
      tooLargeErrorDiv.ariaLive = 'polite'
    }
  }
  if (tooLargeErrorDiv && fileTooLarge && !wasSetTooLarge) {
    // Add extra aria attributes to input if there is an error.
    const errorId = tooLargeErrorDiv.getAttribute('id')
    if (errorId) {
      // Only allow this to be done once so we don't repeatedly append the error id.
      wasSetTooLarge = true
      fileInput.setAttribute('aria-invalid', 'true')
      const ariaDescribedBy = fileInput.getAttribute('aria-describedby') ?? ''
      fileInput.setAttribute(
        'aria-describedby',
        `${errorId} ${ariaDescribedBy}`,
      )
    }
  }
  return fileTooLarge
}
