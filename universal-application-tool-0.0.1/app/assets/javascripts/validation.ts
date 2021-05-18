/** The validation controller provides basic client-side validation of form fields. */
class ValidationController {
  isAddressValid = true;
  isEnumeratorValid = true;
  isNameValid = true;

  constructor() {
    // attach listeners to all form fields.
    this.addQuestionListeners();
  }

  addQuestionListeners() {
    this.addEnumeratorListeners();

  }

  addEnumeratorListeners() {
    // Assumption: There is only ever zero or one enumerators per block.
    const enumeratorQuestion = document.querySelector('.cf-question-enumerator');
    if (enumeratorQuestion) {
      const enumeratorInputs = Array.from(enumeratorQuestion.querySelectorAll('input'))
        .filter(item => item.id !== 'enumerator-delete-template');
      // Whenever an input changes we need to revalidate.
      enumeratorInputs.forEach(enumeratorInput => {
        enumeratorInput.addEventListener("input", () => {
          this.onEnumeratorChanged();
        });
      });

      // Whenever an input is added, we need to add a change listener.
      // mutationObserver.observe(enumeratorQuestion)
    }
  }

  isValid() {
    return this.isAddressValid && this.isEnumeratorValid && this.isNameValid;
  }

  checkAllQuestionTypes() {
    this.isAddressValid = this.validateAddressQuestion();
    this.isEnumeratorValid = this.validateEnumeratorQuestion();
    this.isNameValid = this.validateNameQuestion();
    this.updateSubmitButton();
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
    console.log("Setting submit button to: " + (submitEnabled ? "enabled" : "disabled"));
    const submitButton =
      <HTMLInputElement>document.getElementById('cf-block-submit');
    if (submitButton) {
      submitButton.disabled = !submitEnabled;
    }
  }

  /** Validates that the zip code is in a valid format (5 digit number) */
  validateAddressQuestion(): boolean {
    let isValid = true;
    // for each address question {
    //      validate zip code.
    //      if (zip invalid) { 
    //          // show error message
    //          // isValid = false;
    //      } else {
    //          // hide error message
    //      }
    // }
    return isValid;
  }

  /** Validates that there are no empty or indentical items in the list. */
  validateEnumeratorQuestion(): boolean {
    let isValid = true;
    const enumeratorQuestion = document.querySelector('.cf-question-enumerator');
    if (enumeratorQuestion) {
      const enumeratorInputValues = Array.from(enumeratorQuestion.querySelectorAll('input'))
        .filter(item => item.id !== 'enumerator-delete-template')
        .map(item => item.value);

      // validate that there are no empty inputs.
      const hasEmptyInputs = enumeratorInputValues.includes("");
      // if we have empty inputs then disable the add input button.

      // validate that there are no duplicate entries.
      const hasDupes = (new Set(enumeratorInputValues)).size !== enumeratorInputValues.length;
      isValid = isValid && !hasEmptyInputs && !hasDupes;
    }
    return isValid;
  }

  /** Validates that first and last name are not empty. */
  validateNameQuestion(): boolean {
    let isValid = true;
    // for each name question {
    // Validate first name.
    //      if (first name is empty) { 
    //          // show error message
    //          // isValid = false;
    //      } else {
    //          // hide error message
    //      }
    //
    // Validate last name.
    //      if (last name is empty) { 
    //          // show error message
    //          // isValid = false;
    //      } else {
    //          // hide error message
    //      }
    // }
    return isValid;
  }
}

window.addEventListener('pageshow', () => {
  const validationController = new ValidationController();
  validationController.checkAllQuestionTypes();
});
