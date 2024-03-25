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
  }

  // This event listener should only trigger if the SAVE_ON_ALL_ACTIONS flag is on
  // because the file-upload-action-button class is only added when that flag is on
  // (see {@link views.applicant.ApplicantFileUploadRenderer.java}).
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
  if (uploadedDivs.length) {
    const uploadedDiv: HTMLDivElement = uploadedDivs[0] as HTMLDivElement
    const uploadText = assertNotNull(uploadedDiv.getAttribute(UPLOAD_ATTR))

    blockForm.addEventListener('change', (event) => {
      const files = (event.target! as HTMLInputElement).files
      const file = assertNotNull(files)[0]
      uploadedDiv.innerText = uploadText.replace('{0}', file.name)
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
