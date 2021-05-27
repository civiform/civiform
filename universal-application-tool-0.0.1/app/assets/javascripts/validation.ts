/** The validation controller provides basic client-side validation of form fields. */
class ValidationController {
  /** 
   * Validating on input will call the validators whenever a field is updated.
   * The default behavior is to only attempt to validate on submit.
   * 
   * Validate on input will also disable the submit button when errors are detected.
   */
  static readonly VALIDATE_ON_INPUT = false;

  static readonly ADDRESS_QUESTION_CLASS = '.cf-question-address';
  static readonly ENUMERATOR_QUESTION_CLASS = '.cf-question-enumerator';
  static readonly NAME_QUESTION_CLASS = '.cf-question-name';

  static readonly ENUMERATOR_DELETE_TEMPLATE = 'enumerator-delete-template';
  static readonly BLOCK_SUBMIT_BUTTON_ID = 'cf-block-submit';

  isAddressValid = true;
  isEnumeratorValid = true;
  isNameValid = true;


  constructor() {
    // attach listener to block form.
    const blockForm = document.getElementById("cf-block-form");
    if (blockForm) {
      blockForm.addEventListener("submit", (event) => { return this.attemptSubmit(event); });
    }

    // Maybe attach listeners to all form fields.
    this.addQuestionListeners();
  }

  /** attach listeners so that we know when to update validations. */
  private addQuestionListeners() {
    // Always at least add basic listener for enumerators.
    this.addEnumeratorListeners();

    if (ValidationController.VALIDATE_ON_INPUT) {
      this.addAddressListeners();
      this.addNameListeners();
    }
  }

  private attemptSubmit(event: Event): boolean {
    this.checkAllQuestionTypes();
    if (!this.isValid()) {
      event.preventDefault();
      return false;
    }
    return true;
  }

  /** Add listeners to all address inputs to update validation on changes. */
  private addAddressListeners() {
    const addressQuestions = Array.from(document.querySelectorAll(ValidationController.ADDRESS_QUESTION_CLASS));
    for (const question of addressQuestions) {
      const addressInputs = Array.from(question.querySelectorAll('input'));
      // Whenever an input changes we need to revalidate.
      addressInputs.forEach(addressInput => {
        addressInput.addEventListener("input", () => { this.onAddressChanged(); });
      });
    }
  }

  /** Add listeners to all enumerator inputs to update validation on changes. */
  private addEnumeratorListeners() {
    // Assumption: There is only ever zero or one enumerators per block.
    const enumeratorQuestion = document.querySelector(ValidationController.ENUMERATOR_QUESTION_CLASS);
    if (enumeratorQuestion) {
      const enumeratorInputs = Array.from(enumeratorQuestion.querySelectorAll('input'))
        .filter(item => item.id !== ValidationController.ENUMERATOR_DELETE_TEMPLATE);
      // Whenever an input changes we need to revalidate.
      enumeratorInputs.forEach(enumeratorInput => {
        enumeratorInput.addEventListener("input", () => {
          ValidationController.VALIDATE_ON_INPUT ?
            this.onEnumeratorChanged() :
            this.maybeHideEnumeratorAddButton();
        });
      });

      // Whenever an input is added, we need to add a change listener.
      let mutationObserver = new MutationObserver((records: MutationRecord[]) => {
        for (const record of records) {
          for (const newNode of Array.from(record.addedNodes)) {
            const newInputs = Array.from((<Element>newNode).querySelectorAll('input'));
            newInputs.forEach(newInput => {
              newInput.addEventListener("input", () => {
                ValidationController.VALIDATE_ON_INPUT ?
                  this.onEnumeratorChanged() :
                  this.maybeHideEnumeratorAddButton();
              });
            });
          }
        }
        ValidationController.VALIDATE_ON_INPUT ?
          this.onEnumeratorChanged() :
          this.maybeHideEnumeratorAddButton();
      });

      mutationObserver.observe(enumeratorQuestion,
        {
          childList: true,
          subtree: true,
          characterDataOldValue: true
        });
    }
  }

  /** Add listeners to all address inputs to update validation on changes. */
  private addNameListeners() {
    const addressQuestions = Array.from(document.querySelectorAll(ValidationController.NAME_QUESTION_CLASS));
    for (const question of addressQuestions) {
      const addressInputs = Array.from(question.querySelectorAll('input'));
      // Whenever an input changes we need to revalidate.
      addressInputs.forEach(addressInput => {
        addressInput.addEventListener("input", () => { this.onNameChanged(); });
      });
    }
  }

  checkAllQuestionTypes() {
    this.isAddressValid = this.validateAddressQuestion();
    this.isEnumeratorValid = this.validateEnumeratorQuestion();
    this.isNameValid = this.validateNameQuestion();
    this.updateSubmitButton();
  }

  isValid() {
    return this.isAddressValid && this.isEnumeratorValid && this.isNameValid;
  }

  onAddressChanged() {
    this.isAddressValid = this.validateAddressQuestion();
    this.updateSubmitButton();
  }

  onEnumeratorChanged() {
    this.isEnumeratorValid = this.validateEnumeratorQuestion();
    this.updateSubmitButton();
  }

  onNameChanged() {
    this.isNameValid = this.validateNameQuestion();
    this.updateSubmitButton();
  }

  updateSubmitButton() {
    const submitEnabled = this.isValid();
    const submitButton =
      <HTMLInputElement>document.getElementById(ValidationController.BLOCK_SUBMIT_BUTTON_ID);
    if (submitButton && ValidationController.VALIDATE_ON_INPUT) {
      submitButton.disabled = !submitEnabled;
    }
  }

  updateFieldErrorState(question: Element, fieldName: string, isValid: boolean) {
    const errorDiv = question.querySelector(fieldName + '-error');
    if (errorDiv) {
      errorDiv.classList.toggle('hidden', isValid);
    }

    // Also toggle the border on error inputs (if applicable).
    const field = question.querySelector(fieldName + ' input');
    if (field) {
      field.classList.toggle('border-red-600', !isValid);
    }
  }

  /** Validates all address questions. */
  validateAddressQuestion(): boolean {
    let isValid = true;
    const addressQuestions = Array.from(document.querySelectorAll(ValidationController.ADDRESS_QUESTION_CLASS));
    for (const question of addressQuestions) {
      // validate address line 1 not empty.
      const addressLine1 = <HTMLInputElement>question.querySelector(".cf-address-street-1 input");
      const addressLine1Valid = addressLine1.value.length > 0;
      // Change styling of '.cf-address-street-1-error' as necessary.
      this.updateFieldErrorState(question, '.cf-address-street-1', addressLine1Valid);

      // validate city not empty.
      const city = <HTMLInputElement>question.querySelector(".cf-address-city input");
      const cityValid = city.value.length > 0;
      // Change styling of '.cf-address-city-error' as necessary.
      this.updateFieldErrorState(question, '.cf-address-city', cityValid);

      // validate state.
      const state = <HTMLInputElement>question.querySelector(".cf-address-state input");
      const stateValid = state.value.length > 0;
      // Change styling of '.cf-address-state-error' as necessary.
      this.updateFieldErrorState(question, '.cf-address-state', stateValid);

      // validate zip code.
      const zipCode = <HTMLInputElement>question.querySelector(".cf-address-zip input");
      const hasValidZip = zipCode.value.length == 5 && /^\d+$/.test(zipCode.value);
      // Change styling of '.cf-address-zip-error' as necessary.
      this.updateFieldErrorState(question, '.cf-address-zip', hasValidZip);

      const hasEmptyInputs = !(addressLine1Valid && cityValid && stateValid && zipCode.value.length > 0);

      isValid = !hasEmptyInputs && hasValidZip;
    }
    return isValid;
  }

  /** if we have empty inputs then disable the add input button. (We don't need two blank inputs.) */
  maybeHideEnumeratorAddButton(): boolean {
    let hasEmptyInputs = false;
    const enumeratorQuestion =
      document.querySelector(ValidationController.ENUMERATOR_QUESTION_CLASS);
    if (enumeratorQuestion) {
      const enumeratorInputValues = Array.from(enumeratorQuestion.querySelectorAll('input'))
        .filter(item => item.id !== ValidationController.ENUMERATOR_DELETE_TEMPLATE)
        .map(item => item.value);

      // validate that there are no empty inputs.
      hasEmptyInputs = enumeratorInputValues.includes("");
      const addButton =
        <HTMLInputElement>document.getElementById('enumerator-field-add-button');
      if (addButton) {
        addButton.disabled = hasEmptyInputs;
      }
    }
    return hasEmptyInputs;
  }

  /** Validates that there are no empty or indentical items in the list. */
  validateEnumeratorQuestion(): boolean {
    let isValid = true;
    const enumeratorQuestion = document.querySelector(ValidationController.ENUMERATOR_QUESTION_CLASS);
    if (enumeratorQuestion) {
      const enumeratorInputValues = Array.from(enumeratorQuestion.querySelectorAll('input'))
        .filter(item => item.id !== ValidationController.ENUMERATOR_DELETE_TEMPLATE)
        .map(item => item.value);

      // validate that there are no empty inputs.
      const hasEmptyInputs = this.maybeHideEnumeratorAddButton();

      // validate that there are no duplicate entries.
      const hasDupes = (new Set(enumeratorInputValues)).size !== enumeratorInputValues.length;
      isValid = isValid && !hasEmptyInputs && !hasDupes;


      const errorDiv = enumeratorQuestion.querySelector('.cf-enumerator-error');
      if (errorDiv) {
        errorDiv.classList.toggle('hidden', isValid);
      }
    }
    return isValid;
  }

  /** Validates that first and last name are not empty. */
  validateNameQuestion(): boolean {
    let isValid = true;
    const nameQuestions = Array.from(document.querySelectorAll(ValidationController.NAME_QUESTION_CLASS));
    for (const question of nameQuestions) {
      // validate first name is not empty.
      const firstNameInput = <HTMLInputElement>question.querySelector(".cf-name-first input");
      const firstNameValid = firstNameInput.value.length > 0;
      this.updateFieldErrorState(question, '.cf-name-first', firstNameValid);

      // validate last name is not empty.
      const lastNameInput = <HTMLInputElement>question.querySelector(".cf-name-last input");
      const lastNameValid = lastNameInput.value.length > 0;
      this.updateFieldErrorState(question, '.cf-name-last', lastNameValid);

      isValid = firstNameValid && lastNameValid;
    }
    return isValid;
  }
}

window.addEventListener('pageshow', () => {
  const validationController = new ValidationController();
});
