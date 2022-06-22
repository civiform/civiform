/** The validation controller provides basic client-side validation of admin form fields. */
class AdminValidationController {
  static readonly MULTI_OPTION_QUESTION_FIELD_NAME_CREATE =
    '#question-settings input[name="newOptions[]"]'
  static readonly MULTI_OPTION_QUESTION_FIELD_NAME_EDIT =
    '#question-settings input[name="options[]"]'
  static readonly MULTI_OPTION_QUESTION_ERROR_CLASS =
    '.cf-multi-option-input-error'

  constructor() {
    // Attach listener to admin program edit form.
    const adminProgramEditForm = document.getElementById('question-form')
    if (adminProgramEditForm) {
      adminProgramEditForm.addEventListener('submit', (event) =>
        this.attemptSubmit(event),
      )
    }
  }

  private attemptSubmit(event: Event) {
    if (!this.validateForm()) {
      event.preventDefault()
    }
  }

  private validateForm(): boolean {
    return (
      this.validateMultiOptionQuestionCreate() &&
      this.validateMultiOptionQuestionEdit()
    )
  }

  private updateFieldErrorState(
    element: Element,
    fieldErrorName: string,
    isValid: boolean,
  ) {
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const errorDiv = element.parentElement!.querySelector(fieldErrorName)
    if (errorDiv) {
      errorDiv.classList.toggle('hidden', isValid)
    }

    // Also toggle the border on error inputs (if applicable).
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const field = element.parentElement!.querySelector('input')
    if (field) {
      field.classList.toggle('border-red-600', !isValid)
    }
  }

  /** Validates that there are no empty options. Returns true if all fields are valid.  */
  private validateMultiOptionQuestionOptions(options: HTMLInputElement[]) {
    let multiOptionIsValid = true
    for (const option of options) {
      const inputIsValid = option.value !== ''
      this.updateFieldErrorState(
        option,
        AdminValidationController.MULTI_OPTION_QUESTION_ERROR_CLASS,
        inputIsValid,
      )
      if (!inputIsValid) {
        multiOptionIsValid = inputIsValid
      }
    }
    return multiOptionIsValid
  }

  /** Validates multi option question options when creating a multi option question.  */
  private validateMultiOptionQuestionCreate(): boolean {
    const options = Array.from(
      <NodeListOf<HTMLInputElement>>(
        document.querySelectorAll(
          AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_CREATE,
        )
      ),
    )
    return this.validateMultiOptionQuestionOptions(options)
  }

  /** Validates multi option question options when editing a multi option question.  */
  private validateMultiOptionQuestionEdit(): boolean {
    const options = Array.from(
      <NodeListOf<HTMLInputElement>>(
        document.querySelectorAll(
          AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_EDIT,
        )
      ),
    )
    return this.validateMultiOptionQuestionOptions(options)
  }
}

// eslint-disable-next-line no-unused-vars
const adminValidationController = new AdminValidationController()
