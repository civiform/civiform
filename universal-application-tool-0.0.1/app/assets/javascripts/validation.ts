/** The validation controller provides basic client-side validation of form fields. */
class ValidationController {
  static readonly ADDRESS_QUESTION_CLASS = '.cf-question-address';
  static readonly ENUMERATOR_QUESTION_CLASS = '.cf-question-enumerator';
  static readonly NAME_QUESTION_CLASS = '.cf-question-name';

  static readonly ENUMERATOR_DELETE_TEMPLATE = 'enumerator-delete-template';
  static readonly BLOCK_SUBMIT_BUTTON_ID = 'cf-block-submit';

  isAddressValid = true;
  isEnumeratorValid = true;
  isNameValid = true;

  constructor() {
    // attach listeners to all form fields.
    this.addQuestionListeners();
  }

  /** attach listeners so that we know when to update validations. */
  private addQuestionListeners() {
    this.addAddressListeners();
    this.addEnumeratorListeners();
    this.addNameListeners();
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
          this.onEnumeratorChanged();
        });
      });

      // Whenever an input is added, we need to add a change listener.
      let mutationObserver = new MutationObserver((records: MutationRecord[]) => {
        for (const record of records) {
          for (const newNode of Array.from(record.addedNodes)) {
            const newInputs = Array.from((<Element>newNode).querySelectorAll('input'));
            newInputs.forEach(newInput => {
              newInput.addEventListener("input", () => {
                this.onEnumeratorChanged();
              });
            });
          }
        }
        this.onEnumeratorChanged();
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
    if (submitButton) {
      submitButton.disabled = !submitEnabled;
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

      // validate city not empty.
      const city = <HTMLInputElement>question.querySelector(".cf-address-city input");
      const cityValid = city.value.length > 0;

      // validate state.
      const state = <HTMLInputElement>question.querySelector(".cf-address-state input");
      const stateValid = state.value.length > 0;

      // validate zip code.
      const zipCode = <HTMLInputElement>question.querySelector(".cf-address-zip input");

      const hasEmptyInputs = !(addressLine1Valid && cityValid && stateValid && zipCode.value.length > 0);
      const hasValidZip = zipCode.value.length == 5 && /^\d+$/.test(zipCode.value);

      isValid = !hasEmptyInputs && hasValidZip;
    }
    return isValid;
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
      const hasEmptyInputs = enumeratorInputValues.includes("");
      // if we have empty inputs then disable the add input button. (We don't need two blank inputs.)
      const addButton = <HTMLInputElement>document.getElementById('enumerator-field-add-button');
      if (addButton) {
        addButton.disabled = hasEmptyInputs;
      }

      // validate that there are no duplicate entries.
      const hasDupes = (new Set(enumeratorInputValues)).size !== enumeratorInputValues.length;
      isValid = isValid && !hasEmptyInputs && !hasDupes;
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

      // validate last name is not empty.
      const lastNameInput = <HTMLInputElement>question.querySelector(".cf-name-last input");
      const lastNameValid = lastNameInput.value.length > 0;

      isValid = firstNameValid && lastNameValid;
    }
    return isValid;
  }
}

window.addEventListener('pageshow', () => {
  const validationController = new ValidationController();
  validationController.checkAllQuestionTypes();
});
