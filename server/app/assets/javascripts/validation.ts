/** The validation controller provides basic client-side validation of form fields. */
class ValidationController {
  /**
   * Validating on input will call the validators whenever a field is updated.
   * The default behavior is to only attempt to validate on submit.
   *
   * Validate on input will also disable the submit button when errors are detected.
   */
  static readonly VALIDATE_ON_INPUT = false

  static readonly ENUMERATOR_QUESTION_CLASS = '.cf-question-enumerator'
  static readonly FILEUPLOAD_QUESTION_CLASS = '.cf-question-fileupload'

  static readonly ENUMERATOR_DELETE_TEMPLATE = 'enumerator-delete-template'
  static readonly BLOCK_SUBMIT_BUTTON_ID = 'cf-block-submit'

  isEnumeratorValid = true
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
    // Always at least add basic listener for enumerators.
    this.addEnumeratorListeners()

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

  /** Add listeners to all enumerator inputs to update validation on changes. */
  private addEnumeratorListeners() {
    // Assumption: There is only ever zero or one enumerators per block.
    const enumeratorQuestion = document.querySelector(
      ValidationController.ENUMERATOR_QUESTION_CLASS
    )
    if (enumeratorQuestion) {
      const enumeratorInputs = Array.from(
        enumeratorQuestion.querySelectorAll('input')
      ).filter(
        (item) => item.id !== ValidationController.ENUMERATOR_DELETE_TEMPLATE
      )
      // Whenever an input changes we need to revalidate.
      enumeratorInputs.forEach((enumeratorInput) => {
        enumeratorInput.addEventListener('input', () => {
          ValidationController.VALIDATE_ON_INPUT
            ? this.onEnumeratorChanged()
            : this.maybeHideEnumeratorAddButton()
        })
      })

      // Whenever an input is added, we need to add a change listener.
      let mutationObserver = new MutationObserver(
        (records: MutationRecord[]) => {
          for (const record of records) {
            for (const newNode of Array.from(record.addedNodes)) {
              const newInputs = Array.from(
                (<Element>newNode).querySelectorAll('input')
              )
              newInputs.forEach((newInput) => {
                newInput.addEventListener('input', () => {
                  ValidationController.VALIDATE_ON_INPUT
                    ? this.onEnumeratorChanged()
                    : this.maybeHideEnumeratorAddButton()
                })
              })
            }
          }
          ValidationController.VALIDATE_ON_INPUT
            ? this.onEnumeratorChanged()
            : this.maybeHideEnumeratorAddButton()
        }
      )

      mutationObserver.observe(enumeratorQuestion, {
        childList: true,
        subtree: true,
        characterDataOldValue: true,
      })
    }
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
    this.isEnumeratorValid = this.validateEnumeratorQuestion()
    this.isFileUploadValid = this.validateFileUploadQuestions()
    this.updateSubmitButton()
  }

  isValid() {
    return this.isEnumeratorValid && this.isFileUploadValid
  }

  onEnumeratorChanged() {
    this.isEnumeratorValid = this.validateEnumeratorQuestion()
    this.updateSubmitButton()
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

  /** if we have empty inputs then disable the add input button. (We don't need two blank inputs.) */
  maybeHideEnumeratorAddButton(): boolean {
    let hasEmptyInputs = false
    const enumeratorQuestion = document.querySelector(
      ValidationController.ENUMERATOR_QUESTION_CLASS
    )
    if (enumeratorQuestion) {
      const enumeratorInputValues = Array.from(
        enumeratorQuestion.querySelectorAll('input')
      )
        .filter(
          (item) => item.id !== ValidationController.ENUMERATOR_DELETE_TEMPLATE
        )
        .map((item) => item.value)

      // validate that there are no empty inputs.
      hasEmptyInputs = enumeratorInputValues.includes('')
      const addButton = <HTMLInputElement>(
        document.getElementById('enumerator-field-add-button')
      )
      if (addButton) {
        addButton.disabled = hasEmptyInputs
      }
    }
    return hasEmptyInputs
  }

  /** Validates that there are no empty or indentical items in the list. */
  validateEnumeratorQuestion(): boolean {
    let isValid = true
    const enumeratorQuestion = document.querySelector(
      ValidationController.ENUMERATOR_QUESTION_CLASS
    )
    if (enumeratorQuestion) {
      const enumeratorInputValues = Array.from(
        enumeratorQuestion.querySelectorAll('input')
      )
        .filter(
          (item) => item.id !== ValidationController.ENUMERATOR_DELETE_TEMPLATE
        )
        .map((item) => item.value)

      // validate that there are no empty inputs.
      const hasEmptyInputs = this.maybeHideEnumeratorAddButton()

      // validate that there are no duplicate entries.
      const hasDupes =
        new Set(enumeratorInputValues).size !== enumeratorInputValues.length
      isValid = isValid && !hasEmptyInputs && !hasDupes

      const errorDiv = enumeratorQuestion.querySelector('.cf-enumerator-error')
      if (errorDiv) {
        errorDiv.classList.toggle('hidden', isValid)
      }
    }
    return isValid
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
