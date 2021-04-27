
/** This class controls the style of selected radio buttons. */
 class RadioController {
    static radioInputClass = '.cf-radio-input';
    static radioOptionClass = '.cf-radio-option';
   
    constructor() {
      this.addRadioListeners();
    }
    
    /** Add listeners to radio buttons to change style on selection. */
    addRadioListeners() {
      const radios = Array.from(document.querySelectorAll(RadioController.radioInputClass));
      radios.forEach(
        (radio) => {
            // Add listener to radio button.
            radio.addEventListener('change', () => {
                const buttons = Array.from(document.querySelectorAll(RadioController.radioInputClass));
                for (const radioButton of buttons) {
                    const currentButton = (radioButton as HTMLInputElement);
                    const isChecked = currentButton.checked;
                    const radioContainer = radioButton.closest(RadioController.radioOptionClass);
                    if (radioContainer) {
                        radioContainer.classList.toggle("bg-blue-100", isChecked);
                        radioContainer.classList.toggle("border-blue-400", isChecked);
                    }
                }
            });
      });
    }
  
  }
    
  new RadioController();
  