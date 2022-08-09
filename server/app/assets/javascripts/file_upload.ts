window.addEventListener('load', (_event) => {
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
      errorDiv.classList.toggle('hidden', isValid)
    }
    isAllValid = isAllValid && isValid
  }
  return isAllValid
}
