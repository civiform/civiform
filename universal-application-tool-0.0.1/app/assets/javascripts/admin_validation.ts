/** The validation controller provides basic client-side validation of admin form fields. */
class AdminValidationController {
  static readonly MULTI_OPTION_QUESTION_FIELD_NAME_CREATE = '#question-settings input[name="newOptions[]"]';
  static readonly MULTI_OPTION_QUESTION_FIELD_NAME_EDIT = '#question-settings input[name="options[]"]';

  isMultiOptionCreateValid = true;
  isMultiOptionEditValid = true;

  constructor() {
    // Attach listener to admin program edit form.
    const adminProgramEditForm = document.getElementById("question-form");
    if (adminProgramEditForm) {
      adminProgramEditForm.addEventListener("submit", (event) => { return this.attemptSubmit(event); });
    }
  }

  private attemptSubmit(event: Event): boolean {
    this.checkFields();
    if (!this.isValid()) {
      event.preventDefault();
      return false;
    }
    return true;
  }

  private checkFields() {
    this.isMultiOptionCreateValid = this.validateMultiOptionQuestionCreate();
    this.isMultiOptionEditValid = this.validateMultiOptionQuestionEdit();
  }

  updateFieldErrorState(element: Element, fieldName: string, isValid: boolean) {
    const errorDiv = element.parentElement ? element.parentElement.querySelector(fieldName + '-error') : element.querySelector(fieldName + '-error');
    if (errorDiv) {
      errorDiv.classList.toggle('hidden', isValid);
    }

    // Also toggle the border on error inputs (if applicable).
    const field = element.parentElement ? element.parentElement.querySelector('input') : element.querySelector(fieldName + ' input');
    if (field) {
      field.classList.toggle('border-red-600', !isValid);
    }
  }

  isValid() {
    return this.isMultiOptionCreateValid && this.isMultiOptionEditValid;
  }

  /** Validates that there are no empty options. Returns true if all fields are valid.  */
  validateMultiOptionQuestionOptions(options: HTMLInputElement[]) {
    let multiOptionIsValid = true;
    for (const option of options) {
      const inputIsValid = option.value !== '';
      this.updateFieldErrorState(option, ".cf-multi-option-input", inputIsValid);
      if (!inputIsValid) {
        multiOptionIsValid = inputIsValid;
      }
    }
    return multiOptionIsValid;
  }

  /** Validates multi option question options when creating a multi option question.  */
  validateMultiOptionQuestionCreate(): boolean {
    const options = Array.from(<NodeListOf<HTMLInputElement>>document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_CREATE));
    return this.validateMultiOptionQuestionOptions(options);
  }

  /** Validates multi option question options when editing a multi option question.  */
  validateMultiOptionQuestionEdit(): boolean {
    const options = Array.from(<NodeListOf<HTMLInputElement>>document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_EDIT));
    return this.validateMultiOptionQuestionOptions(options);
  }
}

let adminValidationController = new AdminValidationController();