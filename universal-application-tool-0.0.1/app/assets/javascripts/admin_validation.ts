/** The validation controller provides basic client-side validation of admin form fields. */
class AdminValidationController {

    static readonly MULTI_OPTION_QUESTION_FIELD_NAME = '[name="newOptions[]"]';
    static readonly BLOCK_SUBMIT_BUTTON_ID = 'cf-block-submit';

    isMultiOptionValid = true;

    constructor() {
        // attach listener to admin program edit form.
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
        this.isMultiOptionValid = this.validateMultiOptionQuestion();
        // Option to add this back in. 
        // this.updateSubmitButton();
    }

    isValid() {
        return this.isMultiOptionValid;
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

    /** Validates that there are no empty options.  */
    validateMultiOptionQuestion(): boolean {
        console.log("VALIDATE MULTI OPTION QUESTION HIT");
        let isValid = true;
        const options = Array.from(document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME));
        // Currently, this is unhelpful. options is not what I expected...I am looking to get an array of all the inputted options, e.g. ["Orange", "Green", ""]; So we need to find some more accurate text content getters.
        console.log("MULTI OPTIONS: " + options);
        console.log("SINGLE MULTI OPTION: " + options[0].textContent);
        console.log("SINGLE MULTI OPTION: " + options[1].textContent);

        // TODO: check if any options are empty and return false if so.
        return isValid;
    }

    /** Add listeners to all multi option inputs to update validation on changes. */
  private addMultiOptionListeners() {
    const multiOptionQuestions = Array.from(document.querySelectorAll(AdminValidationController.MULTI_OPTION_QUESTION_FIELD_NAME));
    for (const question of multiOptionQuestions) {
      const multiOptionInputs = Array.from(question.querySelectorAll('input'));
      // Whenever an input changes we need to revalidate.
    //   multiOptionInputs.forEach(multiOptionsPerInput => {
    //     multiOptionsPerInput.forEach(option => {option.addEventListener("input", () => { this.onMultiOptionChanged(); });
    //   }); });
      multiOptionInputs.forEach(multiOptionsPerInput => {
        multiOptionsPerInput.addEventListener("input", () => { this.onMultiOptionChanged(); });
      }); 
    }
  }

  onMultiOptionChanged(){
    this.isMultiOptionValid = this.validateMultiOptionQuestion();
    this.updateSubmitButton();
  }
}

let adminValidationController = new AdminValidationController();