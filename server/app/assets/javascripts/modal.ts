class ModalController {
  /**
   * Find the modals, and add on-click listeners on their respective buttons to toggle them.
   * @param {Element} modalContainer The container holding the modal.
   * @param {Element} modal The modal element.
   **/
  static attachModalListeners(modalContainer: Element, modal: Element) {
    // Connect the modal to its button
    const modalButton = document.querySelector(`#${modal.id}-button`)
    if (modalButton) {
      modalButton.addEventListener('click', (e: Event) => {
        e.stopPropagation()
        ModalController.toggleModal(modalContainer, modal)
      })
    }

    const modalCloses = Array.from(modal.querySelectorAll('.cf-modal-close'))
    modalCloses.forEach((modalCloses) => {
      modalCloses.addEventListener('click', () => {
        ModalController.toggleModal(modalContainer, modal)
      })
    })
  }

  static toggleModal(modalContainer: Element, modal: Element) {
    modalContainer.classList.toggle('hidden')
    modal.classList.toggle('hidden')
  }

  constructor() {
    const modalContainer = document.querySelector('#modal-container')
    if (modalContainer == null) {
      throw new Error('Modal Container display not found!')
    }

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

new ModalController()
