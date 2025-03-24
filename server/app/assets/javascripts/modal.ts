export class ModalController {
  static abortController = new AbortController()

  /**
   * Find the modals, and add on-click listeners on their respective buttons to toggle them.
   * @param {Element} modalContainer The container holding the modal.
   * @param {HTMLElement} modal The modal element.
   **/
  static attachModalListeners(modalContainer: Element, modal: HTMLElement) {
    // Connect the modal to its button
    const modalButton = document.querySelector(`#${modal.id}-button`)
    if (modalButton) {
      modalButton.addEventListener(
        'click',
        (e: Event) => {
          e.stopPropagation()
          ModalController.showModal(modalContainer, modal)
        },
        // Add an abort controller so we can easily clear the click behavior with ModalController.abortController.abort()
        {signal: ModalController.abortController.signal},
      )
    }

    const modalCloses = Array.from(modal.querySelectorAll('.cf-modal-close'))
    modalCloses.forEach((modalCloses) => {
      modalCloses.addEventListener('click', () => {
        ModalController.hideModal(modalContainer, modal)
      })
    })
  }

  static showModal(modalContainer: Element, modal: HTMLElement) {
    if (!this.avoidShowingModalAgain(modal)) {
      modalContainer.classList.remove('hidden')
      modal.classList.remove('hidden')
      modal.focus()
    }
  }

  static hideModal(modalContainer: Element, modal: Element) {
    modalContainer.classList.add('hidden')
    modal.classList.add('hidden')
  }

  /**
   * Checks to see if the modal has an attribute `only-show-once-group`. If so,
   * we must only show this Modal once. We do this by storing a constant key in localStorage
   * that indicates the modal has been shown, and checking for it.
   *
   * In addition, there are some use cases where modals are a middleman for a redirect, with
   * a button to bypass the modal. If there is an attribute `bypass-url` then we will use it to
   * redirect the user to that url, as if they had clicked the bypass element themselves.
   *
   * Returns a boolean indicating whether to skip showing the Modal if these cases are met.
   */
  private static avoidShowingModalAgain(modal: Element): boolean {
    const onlyShowOnceGroup = modal.getAttribute('only-show-once-group')
    if (!onlyShowOnceGroup) return false

    const shownKey = onlyShowOnceGroup
    const modalHasBeenShown = localStorage.getItem(shownKey)
    const bypassUrl = modal.getAttribute('bypass-url')

    if (modalHasBeenShown) {
      if (bypassUrl) window.location.href = bypassUrl
      return true
    }

    localStorage.setItem(shownKey, 'true')
    return false
  }

  constructor() {
    const modalContainer = document.querySelector('#modal-container')
    if (modalContainer == null) {
      throw new Error('Modal Container display not found!')
    }

    const modals = Array.from(
      modalContainer.querySelectorAll<HTMLElement>('.cf-modal'),
    )

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
        ModalController.showModal(modalContainer, modal)
      }
    })

    // Advertise (e.g., for browser tests) that modal.ts initialization is done
    document.body.dataset.loadModal = 'true'
  }
}

export function init() {
  new ModalController()
}

/**
 * Hides the specified modal by adding the 'is-hidden' class.
 * Updates visibility tracking flags when hiding a modal.
 *
 * @param modalType Type of modal to hide
 */
export function hideUswdsModal(modalType: string) {
  const modal = document.getElementById(`${modalType}-modal`)
  if (modal) {
    modal.classList.add('is-hidden')
  }
}
