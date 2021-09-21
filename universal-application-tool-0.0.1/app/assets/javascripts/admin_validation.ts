/** The validation controller provides basic client-side validation of admin form fields. */
class AdminValidationController {

    static readonly MULTI_OPTION_QUESTION_FIELD_NAME_CREATE = '#question-settings input[name="newOptions[]"]';
    static readonly MULTI_OPTION_QUESTION_FIELD_NAME_EDIT = '#question-settings input[name="options[]"]';
    static readonly BLOCK_SUBMIT_BUTTON_ID = 'cf-block-submit';

    isMultiOptionCreateValid = true;
    isMultiOptionEditValid = true;

    constructor() {
        // Attach listener to admin program edit form.
        const adminProgramEditForm = document.getElementById("question-form");
        if (adminProgramEditForm) {
            adminProgramEditForm.addEventListener("submit", (event) => { return this.attemptSubmit(event); });
        }

        // I am still unclear on what the listeners do...but I think they do some validation work before clicking submit. So unsure if we will need them or not.
        // this.addMultiOptionListeners();
        }

    private attemptSubmit(event: Event): boolean {
        console.log("ATTEMPT SUBMIT HIT");
        this.checkFields();
        if (!this.isValid()) {
            event.preventDefault();
            console.log("SUBMIT FAIL");
            return false;
        }
        console.log("SUBMIT SUCCEED");
        return true;
    }

    private checkFields(){
        this.isMultiOptionCreateValid = this.validateMultiOptionQuestionCreate();
        this.isMultiOptionEditValid = this.validateMultiOptionQuestionEdit();
        
        // const createOptions = Array.from(<NodeListOf<HTMLInputElement>>document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_CREATE));
        // for (const createOption of createOptions) {
        //   this.updateFieldErrorState(createOption, ".cf-multi-option-input", this.isMultiOptionCreateValid);
        // }
        // this.updateFieldErrorState(question, "", this.isMultiOptionCreateValid);
        // this.updateFieldErrorState(question, "", this.isMultiOptionEditValid);
        // Option to add this back in. 
        // this.updateSubmitButton();
    }

    updateFieldErrorState(element: Element, fieldName: string, isValid: boolean) {
      const errorDiv = element.parentElement ? element.parentElement.querySelector(fieldName + '-error') : element.querySelector(fieldName + '-error');
      if (errorDiv) {
        errorDiv.classList.toggle('hidden', isValid);
      }
  
      // Also toggle the border on error inputs (if applicable).
      const field = element.querySelector(fieldName + ' input');
      if (field) {
        field.classList.toggle('border-red-600', !isValid);
      }
    }

    isValid() {
      return this.isMultiOptionCreateValid && this.isMultiOptionEditValid;
    }

    // Currently not calling this.
    updateSubmitButton() {
        const submitEnabled = this.isValid();
        const submitButton =
            <HTMLInputElement>document.getElementById(AdminValidationController.BLOCK_SUBMIT_BUTTON_ID);
            console.log("submit updated: " + submitEnabled)
        if (submitButton) {
            submitButton.disabled = !submitEnabled;
        }
    }

    /** Validates that there are no empty options. Returns true if all fields are valid.  */
    validateMultiOptionQuestionCreate(): boolean {
      const options = Array.from(<NodeListOf<HTMLInputElement>>document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_CREATE));
      const isValid = !options.some(option =>  option.value === '');    
      for (const option of options) {
        const isInputValid = option.value !== '';
        this.updateFieldErrorState(option, ".cf-multi-option-input", isInputValid);
      }
      return isValid;
    }

    /** Validates that there are no empty options. Returns true if all fields are valid.  */
    validateMultiOptionQuestionEdit(): boolean {
      const options = Array.from(<NodeListOf<HTMLInputElement>>document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_EDIT));
      const isValid = !options.some(option =>  option.value === '');    
      for (const option of options) {
        const isInputValid = option.value !== '';
        this.updateFieldErrorState(option, ".cf-multi-option-input", isInputValid);
      }
      return isValid;
    }

    /** Add listeners to all multi option inputs to update validation on changes. */
  private addMultiOptionListeners() {
    const multiOptionQuestions = Array.from(document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_CREATE));
    for (const question of multiOptionQuestions) {
      const multiOptionInputs = Array.from(question.querySelectorAll('input'));
      // Whenever an input changes we need to revalidate.
    //   multiOptionInputs.forEach(multiOptionsPerInput => {
    //     multiOptionsPerInput.forEach(option => {option.addEventListener("input", () => { this.onMultiOptionChanged(); });
    //   }); });
      // multiOptionInputs.forEach(multiOptionsPerInput => {
      //   multiOptionsPerInput.addEventListener("input", () => { this.onMultiOptionChanged(); });
      // }); 
    }
  }

  // Not used right now
  // onMultiOptionChanged(){
  //   this.isMultiOptionCreateValid = this.validateMultiOptionQuestion();
  //   this.updateSubmitButton();
  // }
}

let adminValidationController = new AdminValidationController();