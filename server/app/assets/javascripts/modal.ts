/**
 */

class ModalController {
  /** Find the modals, and add on-click listeners on their respective buttons to toggle them. */
  static attachModalListeners(modalContainer: Element, modal: Element) {
    // Connect the modal to its button.
    const modalButton = document.querySelector(`#${modal.id}-button`)
    if (modalButton) {
      modalButton.addEventListener('click', function () {
        ModalController.toggleModal(modalContainer, modal)
      })
    }

    // Connect the modal to any other elements that have an attribute signify
    // they can open a given modal.
    const modalOpeners = Array.from(document.querySelectorAll(`[data-modal-opener-for="${modal.id}"]`));
    modalOpeners.forEach((modalOpener) => {
      modalOpener.addEventListener('click', function() {
        ModalController.toggleModal(modalContainer, modal)
      })
    })

    const modalClose = document.querySelector(`#${modal.id}-close`)
    if (modalClose) {
      modalClose.addEventListener('click', function () {
        ModalController.toggleModal(modalContainer, modal)
      })
    }
  }

  static toggleModal(modalContainer: Element, modal: Element) {
    modalContainer.classList.toggle('hidden')
    modal.classList.toggle('hidden')
  }

  constructor() {
    const modalContainer = document.querySelector('#modal-container')!
    const modals = Array.from(modalContainer.querySelectorAll('.cf-modal'))

    let alreadyDisplayedModalOnLoad = false
    modals.forEach((modal) => {
      ModalController.attachModalListeners(modalContainer, modal)

      if (modal.classList.contains('cf-modal-display-on-load')) {
        if (alreadyDisplayedModalOnLoad) {
          console.error(
            'Multiple modal dialogs requested to be displayed on load. Only displaying the first',
          )
          return
        }
        alreadyDisplayedModalOnLoad = true
        ModalController.toggleModal(modalContainer, modal)
      }
    })

    // Advertise (e.g., for browser tests) that modal.ts initialization is done
    document.body.dataset.loadModal = 'true'
  }
}

let modalController = new ModalController()
