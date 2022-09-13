const fileUploadScriptEl = document.currentScript

window.addEventListener('load', () => {
  // Prevent attempting to submit a file upload form
  // if no file has been selected. Note: For optional
  // file uploads, a distinct skip button is shown.
  const blockForm = document.getElementById('cf-block-form')
  if (blockForm) {
    blockForm.addEventListener('submit', (event) => {
      if (!validateFileUploadQuestions(blockForm)) {
        event.preventDefault()
        return false
      }
      return true
    })
  }

  // Advertise for browser tests that initialization is done.
  if (fileUploadScriptEl) {
    fileUploadScriptEl.setAttribute('data-has-loaded', 'true')
  }
})

function validateFileUploadQuestions(formEl: Element): boolean {
  let isAllValid = true
  const questions = Array.from(
    formEl.querySelectorAll('.cf-question-fileupload'),
  )
  for (const question of questions) {
    // validate a file is selected.
    const fileInput = <HTMLInputElement>(
      question.querySelector('input[type=file]')
    )
    const isValid = fileInput.value != ''

    // Toggle the error div if invalid.
    const errorDiv = question.querySelector('.cf-fileupload-error')
    if (errorDiv) {
      // TODO(#1878): Update button aria attributes on error.
      errorDiv.classList.toggle('hidden', isValid)
    }
    isAllValid = isAllValid && isValid
  }
  return isAllValid
}
