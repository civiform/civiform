export class NorthStarModalController {
  /**
   * Find the modals, and add on-click listeners on their respective buttons to toggle them.
   * @param {Element} modalContainer The container holding the modal.
   * @param {HTMLElement} modal The modal element.
   **/
  static attachModalListener(modal: HTMLElement) {
    // When the button is clicked to trigger the modal, save to local storage that that modal type
    // has been shown.
    const onlyShowOnceGroup = modal.getAttribute('only-show-once-group')
    const modalButtons = document.querySelectorAll(`a[href='#${modal.id}']`)
    if (onlyShowOnceGroup) {
      modalButtons.forEach((modalButton) => {
        modalButton.addEventListener('click', () => {
          localStorage.setItem(onlyShowOnceGroup, 'true')
          NorthStarModalController.maybeUpdateButtonHrefToBypassUrl(modal)
        })
      })
    }
  }

  static maybeUpdateButtonHrefToBypassUrl(modal: HTMLElement) {
    const onlyShowOnceGroup = modal.getAttribute('only-show-once-group')
    if (onlyShowOnceGroup) {
      const modalHasBeenShown = localStorage.getItem(onlyShowOnceGroup)
      const bypassUrl = modal.getAttribute('bypass-url')
      if (modalHasBeenShown) {
        // Find corresponding buttons that open this modal and update href to bypassUrl
        const modalButtons = document.querySelectorAll<HTMLAnchorElement>(
          `a[href='#${modal.id}']`,
        )
        modalButtons.forEach((modalButton) => {
          if (bypassUrl) {
            modalButton.href = bypassUrl
          }
        })
      }
    }
  }

  constructor() {
    const modalContainer = document.querySelector('#modal-container')
    if (modalContainer == null) {
      throw new Error('Modal Container display not found!')
    }

    const modals = Array.from(
      modalContainer.querySelectorAll<HTMLElement>('.cf-ns-modal'),
    )

    modals.forEach((modal) => {
      NorthStarModalController.attachModalListener(modal)

      NorthStarModalController.maybeUpdateButtonHrefToBypassUrl(modal)
    })

    // Advertise (e.g., for browser tests) that modal.ts initialization is done
    document.body.dataset.loadModal = 'true'
  }
}

export function init() {
  new NorthStarModalController()
}
