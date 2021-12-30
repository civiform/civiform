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
  static readonly CURRENCY_QUESTION_CLASS = '.cf-question-currency';
  static readonly CURRENCY_VALUE_CLASS = '.cf-currency-value';
  static readonly ENUMERATOR_QUESTION_CLASS = '.cf-question-enumerator';
  static readonly FILEUPLOAD_QUESTION_CLASS = '.cf-question-fileupload';
  static readonly NAME_QUESTION_CLASS = '.cf-question-name';
  static readonly REQUIRED_QUESTION_CLASS = 'cf-question-required';

  static readonly ENUMERATOR_DELETE_TEMPLATE = 'enumerator-delete-template';
  static readonly BLOCK_SUBMIT_BUTTON_ID = 'cf-block-submit';

  // Currency validation regexs.  Note: there are backend versions that need
  // to stay in sync in app/services/applicant/Currency.java
  // Currency containing only numbers, without leading 0s and optional 2 digit cents.
  static readonly CURRENCY_NO_COMMAS = /^[1-9]\d*(?:\.\d\d)?$/;
  // Same as CURRENCY_NO_COMMAS but commas followed by 3 digits are allowed.
  static readonly CURRENCY_WITH_COMMAS = /^[1-9]\d{0,2}(?:,\d\d\d)*(?:\.\d\d)?$/;
  // Currency of 0 dollars with optional 2 digit cents.
  static readonly CURRENCY_ZERO_DOLLARS = /^0(?:\.\d\d)?$/;

  isAddressValid = true;
  isCurrencyValid = true;
  isEnumeratorValid = true;
  isFileUploadValid = true;
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
      this.addCurrencyListeners();
      this.addFileUploadListener();
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

  /** Add listeners to all currency inputs to update validation on changes. */
  private addCurrencyListeners() {
    const currencyInputs = Array.from(document.querySelectorAll(`${ValidationController.CURRENCY_QUESTION_CLASS} input`));
    currencyInputs.forEach(currencyInput => currencyInput.addEventListener("input", () => this.onCurrencyChanged()));

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

  /** Add listeners to file input to update validation on changes. */
  private addFileUploadListener() {
    // Assumption: There is only ever zero or one file input per block.
    const fileQuestion = document.querySelector(ValidationController.FILEUPLOAD_QUESTION_CLASS);
    if (fileQuestion) {
      const fileInput = fileQuestion.querySelector('input[type=file]');
      // Whenever the input changes we need to revalidate.
      if (fileInput) {
        fileInput.addEventListener("input", () => { this.onFileChanged(); });
      }
    }
  }

  checkAllQuestionTypes() {
    this.isAddressValid = this.validateAddressQuestion();
    this.isCurrencyValid = this.validateCurrencyQuestion();
    this.isEnumeratorValid = this.validateEnumeratorQuestion();
    this.isFileUploadValid = this.validateFileUploadQuestion();
    this.isNameValid = this.validateNameQuestion();
    this.updateSubmitButton();
  }

  isValid() {
    return this.isAddressValid && this.isCurrencyValid && this.isEnumeratorValid
      && this.isFileUploadValid && this.isNameValid;
  }

  onAddressChanged() {
    this.isAddressValid = this.validateAddressQuestion();
    this.updateSubmitButton();
  }

  onCurrencyChanged() {
    this.isCurrencyValid = this.validateCurrencyQuestion();
    this.updateSubmitButton();
  }

  onEnumeratorChanged() {
    this.isEnumeratorValid = this.validateEnumeratorQuestion();
    this.updateSubmitButton();
  }

  onFileChanged() {
    this.isFileUploadValid = this.validateFileUploadQuestion();
    this.updateSubmitButton();
  }

  onNameChanged() {
    this.isNameValid = this.validateNameQuestion();
    this.updateSubmitButton();
  }

  updateSubmitButton() {
    const submitEnabled = this.isValid();
    const submitButton =
      <HTMLInputElemedocument.getElementById(ValidationController.BLOCK_SUBMIT_BUTTON_ID);
    if (submitButton && ValidationController.VALIDATE_ON_INPUT) {
      submitButton.disabled = !submitEnabled;
    }
  }

  updateFieldErrorState(question: Element, fieldName: string, isValid: boolean) {
    const isOptional = !question.classList.contains(ValidationController.REQUIRED_QUESTION_CLASS);
    const filledInputs = Array.from(question.querySelectorAll('input')).filter(input => input.value !== "");
    if (isOptional && filledInputs.length === 0) {
      return;
    }

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
    let allValid = true;
    const addressQuestions = Array.from(document.querySelectorAll(ValidationController.ADDRESS_QUESTION_CLASS));
    for (const question of addressQuestions) {
      // validate address line 1 not empty.
      const addressLine1 = <HTMLInputElement>question.querySelector(".cf-address-street-1 input");
      const addressLine1Empty = addressLine1.value.length == 0;
      const addressLine1Valid = !addressLine1Empty;
      // Change styling of '.cf-address-street-1-error' as necessary.
      this.updateFieldErrorState(question, '.cf-address-street-1', addressLine1Valid);

      const addressLine2 = <HTMLInputElement>question.querySelector(".cf-address-street-2 input");
      const addressLine2Empty = addressLine2.value.length == 0;

      // validate city not empty.
      const city = <HTMLInputElement>question.querySelector(".cf-address-city input");
      const cityEmpty = city.value.length == 0;
      const cityValid = !cityEmpty;
      // Change styling of '.cf-address-city-error' as necessary.
      this.updateFieldErrorState(question, '.cf-address-city', cityValid);

      // validate state.
      const state = <HTMLInputElement>question.querySelector(".cf-address-state input");
      const stateEmpty = state.value.length == 0;
      const stateValid = !stateEmpty;
      // Change styling of '.cf-address-state-error' as necessary.
      this.updateFieldErrorState(question, '.cf-address-state', stateValid);

      // validate zip code.
      const zipCode = <HTMLInputElement>question.querySelector(".cf-address-zip input");
      const zipEmpty = zipCode.value.length == 0;
      const hasValidZip = zipCode.value.length == 5 && /^\d+$/.test(zipCode.value);
      // Change styling of '.cf-address-zip-error' as necessary.
      this.updateFieldErrorState(question, '.cf-address-zip', hasValidZip);

      const hasEmptyInputs = addressLine1Empty || cityEmpty || stateEmpty || zipEmpty;
      const hasValidPresentInputs = !hasEmptyInputs && hasValidZip;

      // If this question isn't required then it's also valid if it is empty.
      const isOptional = !question.classList.contains(ValidationController.REQUIRED_QUESTION_CLASS);
      const emptyOptional = isOptional && addressLine1Empty && addressLine2Empty && cityEmpty && stateEmpty && zipEmpty;

      const isValid = emptyOptional || hasValidPresentInputs;
      allValid = allValid && isValid;
    }
    return allValid;
  }

  /**
   * Validates that the value is present and in integer format (optionally with
   * commas), optionally with exactly 2 decimals.
   */
  validateCurrencyQuestion(): boolean {
    let isAllValid = true;
    const questions = Array.from(document.querySelectorAll(ValidationController.CURRENCY_QUESTION_CLASS));
    for (const question of questions) {
      const currencyInput = <HTMLInputElement>question.querySelector("input[currency]");
      const currencyValue = currencyInput.value;

      const isValidCurrency = ValidationController.CURRENCY_NO_COMMAS.test(currencyValue) ||
        ValidationController.CURRENCY_WITH_COMMAS.test(currencyValue) ||
        ValidationController.CURRENCY_ZERO_DOLLARS.test(currencyValue);

      // If this question isn't required then it's also valid if it is empty.
      const isEmpty = currencyValue.length === 0;
      const isOptional = !question.classList.contains(ValidationController.REQUIRED_QUESTION_CLASS);
      const emptyOptional = isOptional && isEmpty;

      const isValid = emptyOptional || isValidCurrency;
      this.updateFieldErrorState(question, ValidationController.CURRENCY_VALUE_CLASS, isValid);
      isAllValid = isAllValid && isValid;
    }
    return isAllValid;
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

  /** Validates that a file is selected. */
  validateFileUploadQuestion(): boolean {
    let isValid = true;
    const fileUploadQuestion = document.querySelector(ValidationController.FILEUPLOAD_QUESTION_CLASS);
    if (fileUploadQuestion) {
      // validate a file is selected.
      const fileInput = <HTMLInputElement>fileUploadQuestion.querySelector("input[type=file]");
      isValid = fileInput.value != '';

      // Toggle the error div if invalid.
      const errorDiv = fileUploadQuestion.querySelector('.cf-fileupload-error');
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
      const firstNameEmpty = firstNameInput.value.length == 0;
      this.updateFieldErrorState(question, '.cf-name-first', !firstNameEmpty);

      // validate last name is not empty.
      const lastNameInput = <HTMLInputElement>question.querySelector(".cf-name-last input");
      const lastNameEmpty = lastNameInput.value.length == 0;
      this.updateFieldErrorState(question, '.cf-name-last', !lastNameEmpty);

      // check if middle name is empty.
      const middleNameInput = <HTMLInputElement>question.querySelector(".cf-name-middle input");
      const middleNameEmpty = middleNameInput.value.length == 0;

      // If this question isn't required then it's also valid if it is empty.
      const isOptional = !question.classList.contains(ValidationController.REQUIRED_QUESTION_CLASS);
      const emptyOptional = isOptional && firstNameEmpty && lastNameEmpty && middleNameEmpty;

      // TODO: Fix bug where only the last questions validity is used.
      isValid = emptyOptional || (!firstNameEmpty && !lastNameEmpty);
    }
    return isValid;
  }
}

window.addEventListener('pageshow', () => {
  const validationController = new ValidationController();
});