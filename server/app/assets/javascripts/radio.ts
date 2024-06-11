/** This class controls the style of selected radio buttons. */
import {assertNotNull} from './util'

class RadioController {
  static radioDefaultClass = '.cf-radio-default'
  static radioInputClass = '.cf-radio-input'
  static radioOptionClass = '.cf-radio-option'
  static selectedRadioClasses = ['border-civiform-blue', 'bg-blue-200']
  static unselectedRadioClasses = ['border-gray-500', 'bg-white']

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
        const radioName = assertNotNull(targetElement.getAttribute('name'))
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

            // Prevents conflicting border classes from being applied.
            RadioController.unselectedRadioClasses.forEach((unselectedClass) =>
              radioContainer.classList.toggle(unselectedClass, !isChecked),
            )
          }
        }
        // If this is a checkbox we need to check or uncheck the "None selected" option.
        if (targetElement.type === 'checkbox') {
          const defaultCheckbox = document.querySelector<HTMLInputElement>(
            RadioController.radioDefaultClass + "[name='" + radioName + "']",
          )
          if (defaultCheckbox !== null) {
            defaultCheckbox.checked = checkCount == 0
          }
        }
      })
    })
  }
}

export function init() {
  window.addEventListener('pageshow', () => RadioController.initializeRadios())
  new RadioController()
}
