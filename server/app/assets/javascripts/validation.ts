/** The validation controller provides basic client-side validation of form fields. */
class ValidationController {
  /**
   * Validating on input will call the validators whenever a field is updated.
   * The default behavior is to only attempt to validate on submit.
   *
   * Validate on input will also disable the submit button when errors are detected.
   */
  static readonly VALIDATE_ON_INPUT = false

  static readonly FILEUPLOAD_QUESTION_CLASS = '.cf-question-fileupload'

  static readonly BLOCK_SUBMIT_BUTTON_ID = 'cf-block-submit'

  isFileUploadValid = true

  constructor() {
    // attach listener to block form.
    const blockForm = document.getElementById('cf-block-form')
    if (blockForm) {
      blockForm.addEventListener('submit', (event) => {
        return this.attemptSubmit(event)
      })
    }

    // Maybe attach listeners to all form fields.
    this.addQuestionListeners()
  }

  /** attach listeners so that we know when to update validations. */
  private addQuestionListeners() {
    if (ValidationController.VALIDATE_ON_INPUT) {
      this.addFileUploadListener()
    }
  }

  private attemptSubmit(event: Event): boolean {
    this.checkAllQuestionTypes()
    if (!this.isValid()) {
      event.preventDefault()
      return false
    }
    return true
  }

  /** Add listeners to file input to update validation on changes. */
  private addFileUploadListener() {
    const fileQuestions = Array.from(
      <NodeListOf<HTMLInputElement>>(
        document.querySelectorAll(
          `${ValidationController.FILEUPLOAD_QUESTION_CLASS} input[type=file]`
        )
      )
    )
    fileQuestions.forEach((fileQuestion) => {
      fileQuestion.addEventListener('input', () => {
        this.onFileChanged()
      })
    })
  }

  checkAllQuestionTypes() {
    this.isFileUploadValid = this.validateFileUploadQuestions()
    this.updateSubmitButton()
  }

  isValid() {
    return this.isFileUploadValid
  }

  onFileChanged() {
    this.isFileUploadValid = this.validateFileUploadQuestions()
    this.updateSubmitButton()
  }

  updateSubmitButton() {
    const submitEnabled = this.isValid()
    const submitButton = <HTMLInputElement>(
      document.getElementById(ValidationController.BLOCK_SUBMIT_BUTTON_ID)
    )
    if (submitButton && ValidationController.VALIDATE_ON_INPUT) {
      submitButton.disabled = !submitEnabled
    }
  }

  /** Validates that a file is selected. */
  validateFileUploadQuestions(): boolean {
    let isAllValid = true
    const questions = Array.from(
      document.querySelectorAll(ValidationController.FILEUPLOAD_QUESTION_CLASS)
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
}

window.addEventListener('pageshow', () => {
  const validationController = new ValidationController()
})
