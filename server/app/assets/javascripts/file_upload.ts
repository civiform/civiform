import {assertNotNull} from './util'

const UPLOAD_ATTR = 'data-upload-text'
const REVIEW_BUTTON_ID = 'review-application-button';

export function init() {
  // Prevent attempting to submit a file upload form
  // if no file has been selected. Note: For optional
  // file uploads, a distinct skip button is shown.
  const blockForm = document.getElementById('cf-block-form')

  if (blockForm) {
    const uploadedDivs = blockForm.querySelectorAll(`[${UPLOAD_ATTR}]`)

    blockForm.addEventListener('submit', (event) => {
      if (!validateFileUploadQuestions(blockForm)) {
        event.preventDefault()
        return false
      }
      return true
    })

    if (uploadedDivs.length) {
      const uploadedDiv = uploadedDivs[0]
      const uploadText = assertNotNull(uploadedDiv.getAttribute(UPLOAD_ATTR))

      blockForm.addEventListener('change', (event) => {
        const files = (event.target! as HTMLInputElement).files
        const file = assertNotNull(files)[0]
        uploadedDiv.innerHTML = uploadText.replace('{0}', file.name)
      })
    }
  }
}

let wasSetInvalid = false

function validateFileUploadQuestions(formEl: Element): boolean {
  let isAllValid = true
  const questions = Array.from(
    formEl.querySelectorAll('.cf-question-fileupload'),
  )
  for (const question of questions) {
    // validate a file is selected.
    const fileInput = assertNotNull(
      question.querySelector<HTMLInputElement>('input[type=file]'),
    )

    const isValid = fileInput.value != ''

    const errorDiv = question.querySelector('.cf-fileupload-error')
    if (errorDiv) {
      // Toggle the error div if invalid.
      errorDiv.classList.toggle('hidden', isValid)
      if (!isValid) {
        // Add ariaLive label so error is announced to screen reader.
        errorDiv.ariaLive = 'polite'
      }
    }
    if (errorDiv && !isValid && !wasSetInvalid) {
      // Add extra aria attributes to input if there is an error.
      const errorId = errorDiv.getAttribute('id')
      if (errorId) {
        // Only allow this to be done once so we don't repeatedly append the error id.
        wasSetInvalid = true
        fileInput.setAttribute('aria-invalid', 'true')
        const ariaDescribedBy = fileInput.getAttribute('aria-describedby') ?? ''
        fileInput.setAttribute(
          'aria-describedby',
          `${errorId} ${ariaDescribedBy}`,
        )
      }
    }
    isAllValid = isAllValid && isValid
  }
  return isAllValid
}
