import {assertNotNull} from './util'
import { ModalController } from './modal'


class AdminQuestionEdit {
    
  
    constructor() {
        // get the initial toggle value
        const toggleElement = assertNotNull(document.getElementById("universal-toggle-input")) as HTMLInputElement
        console.log(toggleElement)
        const initialToggleValue = toggleElement.value

        const modalContainer = assertNotNull(document.getElementById("modal-container"))
        const modal = assertNotNull(document.getElementById("confirm-question-updates-modal"))
        // remove the default event listener
        ModalController.removeShowModalListener(modalContainer, modal);
        // hmm it's still opening this

        const button = assertNotNull(document.getElementById("confirm-question-updates-modal-button"))
        // add a new click listener that checks if the toggle went from on to off
        button.addEventListener('click', (e: Event) => {
            e.stopPropagation() // do we need this?
            // get the new toggle value
            const currentToggleValue = toggleElement.value
            console.log(initialToggleValue)
            console.log(currentToggleValue)
        })
        // if it did, show the modal
        // if it didn't, click the hidden submit button

        // do i need to prevent this from running when the flag is off? will it affect anything?
        // probably not because the modal just won't be on the page
      
    }
}
  
  export function init() {
    new AdminQuestionEdit()
  }
  