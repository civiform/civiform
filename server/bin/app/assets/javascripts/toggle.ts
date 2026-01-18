/** This class controls the toggling of toggle components. */
import {addEventListenerToElements} from './util'

class ToggleController {
  static toggleButtonClass = '.cf-toggle-button'
  static toggleBackgroundClass = '.cf-toggle-background'
  static toggleNubClass = '.cf-toggle-nub'
  static toggleInputClass = '.cf-toggle-hidden-input'
  static setBackgroundClass = 'bg-blue-600'
  static unsetBackgroundClass = 'bg-gray-600'
  static setNubClass = 'right-1'
  static unsetNubClass = 'left-1'

  constructor() {
    this.addToggleListeners()
  }

  /** Add listeners to toggle buttons to change style on selection. */
  addToggleListeners() {
    addEventListenerToElements(
      ToggleController.toggleButtonClass,
      'click',
      (event: Event) => {
        const target = event.target as HTMLElement
        const toggle = target.closest(
          ToggleController.toggleButtonClass,
        ) as HTMLButtonElement
        const inputElement = toggle.querySelector(
          ToggleController.toggleInputClass,
        ) as HTMLInputElement
        const newValue = inputElement.value === 'false'
        inputElement.value = newValue.toString()

        // Toggle toggle toggle :) The toggle method adds a class if it is missing,
        // and removes it if it's present. We force addition/removal here by providing
        // newValue, just in case the element gets in a weird state somehow where both
        // classes are present/missing at the same time.
        const background = toggle.querySelector(
          ToggleController.toggleBackgroundClass,
        )
        background?.classList.toggle(
          ToggleController.setBackgroundClass,
          newValue,
        )
        background?.classList.toggle(
          ToggleController.unsetBackgroundClass,
          !newValue,
        )

        const nub = toggle.querySelector(ToggleController.toggleNubClass)
        nub?.classList.toggle(ToggleController.setNubClass, newValue)
        nub?.classList.toggle(ToggleController.unsetNubClass, !newValue)
      },
    )
  }
}

export function init() {
  new ToggleController()
}
