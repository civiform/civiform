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
})

let wasSetInvalid = false

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

    const errorDiv = question.querySelector('.cf-fileupload-error')
    if (errorDiv) {
      // Toggle the error div if invalid.
      errorDiv.classList.toggle('hidden', isValid)
    }
    if (errorDiv && !isValid && !wasSetInvalid) {
      // Add extra aria attributes to input if there is an error.
      const errorId = errorDiv.getAttribute('id')
      if (errorId) {
        // Only allow this to be done once so we don't repeatedly append the error id.
        wasSetInvalid = true
        fileInput.setAttribute('aria-invalid', 'true')
        const ariaDescribedBy = fileInput.getAttribute('aria-describedBy')
        fileInput.setAttribute(
          'aria-describedBy',
          `${errorId} ${ariaDescribedBy}`,
        )
      }
    }
    isAllValid = isAllValid && isValid
  }
  return isAllValid
}
