
/** This class controls the style of selected radio buttons. */
class RadioController {
  static radioInputClass = '.cf-radio-input';
  static radioOptionClass = '.cf-radio-option';

  constructor() {
    this.addRadioListeners();
  }

  /**
   * Initialize styling on radio buttons.
   *
   * Since the styling is toggled via javascript, we need to run this whenever the page
   * is shown so that the BF cache doesn't put us in a bad state. 
   */
  public static initializeRadios() {
    const radios = Array.from(document.querySelectorAll(RadioController.radioInputClass));
    radios.forEach(
      (radio) => {
        // Apply appropriate styles in case the user clicked the back button or something.
        const container = radio.closest(RadioController.radioOptionClass); 
        const radioChecked =  (radio as HTMLInputElement).checked;
        if (container) {
          container.classList.toggle("bg-blue-100", radioChecked);
          container.classList.toggle("border-blue-400", radioChecked);
        }
      }
    );
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

window.addEventListener('pageshow', () => RadioController.initializeRadios());
new RadioController();
