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
        // Option to add this back in. 
        // this.updateSubmitButton();
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
      return !options.some(option =>  option.value === '');    }

    /** Validates that there are no empty options. Returns true if all fields are valid.  */
    validateMultiOptionQuestionEdit(): boolean {
      const options = Array.from(<NodeListOf<HTMLInputElement>>document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME_EDIT));
      return !options.some(option =>  option.value === '');   }

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