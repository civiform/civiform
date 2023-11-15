import {assertNotNull} from './util'
import { ModalController } from './modal'


class AdminQuestionEdit {
  
    constructor() {
        // get the initial toggle value
        // I'm getting console errors from this, why not the other ones? what's different?
        const toggleElement = assertNotNull(document.getElementById("universal-toggle-input")) as HTMLInputElement
        const initialToggleValue = toggleElement.value

        const modalContainer = assertNotNull(document.getElementById("modal-container"))
        const modal = assertNotNull(document.getElementById("confirm-question-updates-modal"))
        // remove the default event listener
        ModalController.abortSignal.abort();
        const button = assertNotNull(document.getElementById("confirm-question-updates-modal-button"))
        
        // add a new click handler that checks if the toggle went from on to off
        button.addEventListener('click', (e: Event) => {
            e.stopPropagation() // do we need this?
            // get the new toggle value
            const newToggleElement = assertNotNull(document.getElementById("universal-toggle-input")) as HTMLInputElement
            const currentToggleValue = newToggleElement.value
            console.log("currentToggleValue", currentToggleValue)
            console.log("initialToggleValue", initialToggleValue)
            if (initialToggleValue === "true" && currentToggleValue === "false") {
                ModalController.showModal(modalContainer, modal)
            } else {
                // click the hidden button
                const submitButton = assertNotNull(document.getElementById("accept-question-updates-button"))
                submitButton.click()
            }
        })

        // do i need to prevent this from running when the flag is off? will it affect anything?
        // probably not because the modal just won't be on the page
      
    }
}
  
  export function init() {
    new AdminQuestionEdit()
  }
  