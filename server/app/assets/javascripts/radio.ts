/** This class controls the style of selected radio buttons. */
class RadioController {
  static radioDefaultClass = '.cf-radio-default'
  static radioInputClass = '.cf-radio-input'
  static radioOptionClass = '.cf-radio-option'
  static selectedRadioClasses = ['border-seattle-blue', 'bg-blue-200']

  constructor() {
    this.addRadioListeners()
  }

  /**
   * Initialize styling on radio buttons.
   *
   * Since the styling is toggled via javascript, we need to run this whenever the page
   * is shown so that the BF cache doesn't put us in a bad state.
   */
  public static initializeRadios() {
    const radios = Array.from(
      document.querySelectorAll(RadioController.radioInputClass),
    )
    radios.forEach((radio) => {
      // Apply appropriate styles in case the user clicked the back button or something.
      const container = radio.closest(RadioController.radioOptionClass)
      const radioChecked = (radio as HTMLInputElement).checked
      if (container) {
        RadioController.selectedRadioClasses.forEach((selectedClass) =>
          container.classList.toggle(selectedClass, radioChecked),
        )
      }
    })
  }

  /** Add listeners to radio buttons to change style on selection. */
  addRadioListeners() {
    const radios = Array.from(
      document.querySelectorAll(RadioController.radioInputClass),
    )
    radios.forEach((radio) => {
      // Add listener to radio button.
      radio.addEventListener('change', (e) => {
        const targetElement = e.target as HTMLInputElement
        const radioName = targetElement.getAttribute('name')
        let checkCount = 0
        const buttons = Array.from(
          document.querySelectorAll(
            RadioController.radioInputClass + "[name='" + radioName + "']",
          ),
        )
        for (const radioButton of buttons) {
          const currentButton = radioButton as HTMLInputElement
          const isChecked = currentButton.checked
          checkCount += isChecked ? 1 : 0
          const radioContainer = radioButton.closest(
            RadioController.radioOptionClass,
          )
          if (radioContainer) {
            RadioController.selectedRadioClasses.forEach((selectedClass) =>
              radioContainer.classList.toggle(selectedClass, isChecked),
            )
          }
        }
        // If this is a checkbox we need to check or uncheck the "None selected" option.
        if (targetElement.type === 'checkbox') {
          const defaultCheckbox = document.querySelector(
            RadioController.radioDefaultClass + "[name='" + radioName + "']",
          ) as HTMLInputElement
          if (defaultCheckbox !== null) {
            defaultCheckbox.checked = checkCount == 0
            console.log('Number selected for ' + radioName + ': ' + checkCount)
          }
        }
      })
    })
  }
}

window.addEventListener('pageshow', () => RadioController.initializeRadios())
new RadioController()
