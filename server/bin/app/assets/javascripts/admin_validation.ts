/** The validation controller provides basic client-side validation of admin form fields. */

class AdminValidationController {
  static readonly MULTI_OPTION_QUESTION_OPTION_FIELD_NAME_CREATE =
    '#question-settings input[name="newOptions[]"]'
  static readonly MULTI_OPTION_QUESTION_OPTION_ADMIN_FIELD_NAME_CREATE =
    '#question-settings input[name="newOptionAdminNames[]"]'
  static readonly MULTI_OPTION_QUESTION_OPTION_FIELD_NAME_EDIT =
    '#question-settings input[name="options[]"]'
  static readonly MULTI_OPTION_QUESTION_OPTION_ADMIN_FIELD_NAME_EDIT =
    '#question-settings input[name="optionAdminNames[]"]'
  static readonly MULTI_OPTION_QUESTION_OPTION_ERROR_CLASS =
    '.cf-multi-option-input-error'
  static readonly MULTI_OPTION_QUESTION_OPTION_ADMIN_ERROR_CLASS =
    '.cf-multi-option-admin-input-error'

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
    const errorDiv = element.parentElement!.querySelector(fieldErrorName)
    if (errorDiv) {
      errorDiv.classList.toggle('hidden', isValid)
    }

    // Also toggle the border on error inputs (if applicable).
    const field = element.parentElement!.querySelector('input')
    if (field) {
      field.classList.toggle('border-red-600', !isValid)
    }
  }

  /**
   * Validates that there are no empty options. and updates the field error state.
   * @param {HTMLInputElement[]} inputElements All the inputElements to verify
   * @param {string} errorClass The error class to show on error
   *
   * @return {boolean} true if all fields are valid
   *  */
  private validateMultiOptionQuestionOptions(
    inputElements: HTMLInputElement[],
    errorClass: string,
  ) {
    let multiOptionIsValid = true
    for (const inputElement of inputElements) {
      const inputIsValid = inputElement.value !== ''
      this.updateFieldErrorState(inputElement, errorClass, inputIsValid)
      if (!inputIsValid) {
        multiOptionIsValid = inputIsValid
      }
    }
    return multiOptionIsValid
  }

  /**
   * Validates that there are no invalid admin names and updates the field error state.
   * @param {HTMLInputElement[]} inputElements All the inputElements to verify
   * @param {string} errorClass The error class to show on error
   *
   * @return {boolean} true if all fields are valid
   *  */
  private validateMultiOptionQuestionAdminNames(
    inputElements: HTMLInputElement[],
    errorClass: string,
  ) {
    let multiOptionIsValid = true
    for (const inputElement of inputElements) {
      // Assert that the admin name is not blank, and that it only
      // contains 0-9, a-z, _, and -
      const inputIsValid = /^[0-9a-z_-]+$/.test(inputElement.value)
      this.updateFieldErrorState(inputElement, errorClass, inputIsValid)
      if (!inputIsValid) {
        multiOptionIsValid = inputIsValid
      }
    }
    return multiOptionIsValid
  }

  /**
   * Validates multi option question options when creating a multi option question.
   * @return {boolean} true if all fields are valid
   * */
  private validateMultiOptionQuestionCreate(): boolean {
    const options = Array.from(
      document.querySelectorAll<HTMLInputElement>(
        AdminValidationController.MULTI_OPTION_QUESTION_OPTION_FIELD_NAME_CREATE,
      ),
    )
    const optionAdminNames = Array.from(
      document.querySelectorAll<HTMLInputElement>(
        AdminValidationController.MULTI_OPTION_QUESTION_OPTION_ADMIN_FIELD_NAME_CREATE,
      ),
    )
    const optionTextIsValid = this.validateMultiOptionQuestionOptions(
      options,
      AdminValidationController.MULTI_OPTION_QUESTION_OPTION_ERROR_CLASS,
    )
    const optionAdminNamesAreValid = this.validateMultiOptionQuestionAdminNames(
      optionAdminNames,
      AdminValidationController.MULTI_OPTION_QUESTION_OPTION_ADMIN_ERROR_CLASS,
    )
    return optionTextIsValid && optionAdminNamesAreValid
  }

  /**
   * Validates multi option question options when editing a multi option question.
   * @return {boolean} true if all fields are valid
   * */
  private validateMultiOptionQuestionEdit(): boolean {
    const options = Array.from(
      document.querySelectorAll<HTMLInputElement>(
        AdminValidationController.MULTI_OPTION_QUESTION_OPTION_FIELD_NAME_EDIT,
      ),
    )
    const optionAdminNames = Array.from(
      document.querySelectorAll<HTMLInputElement>(
        AdminValidationController.MULTI_OPTION_QUESTION_OPTION_ADMIN_FIELD_NAME_EDIT,
      ),
    )
    const optionTextIsValid = this.validateMultiOptionQuestionOptions(
      options,
      AdminValidationController.MULTI_OPTION_QUESTION_OPTION_ERROR_CLASS,
    )
    const optionAdminNamesAreValid = this.validateMultiOptionQuestionOptions(
      optionAdminNames,
      AdminValidationController.MULTI_OPTION_QUESTION_OPTION_ADMIN_ERROR_CLASS,
    )
    return optionTextIsValid && optionAdminNamesAreValid
  }
}

export function init() {
  new AdminValidationController()
}
