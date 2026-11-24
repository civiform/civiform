import {default as uswdsModal} from '@uswds/uswds/js/usa-modal'

// Named, module-level handlers so that adding them to the same trigger more than
// once is a no-op. addEventListener ignores a listener it already has.
const preventDefaultNavigation = (event: Event) => event.preventDefault()
const toggleUswdsModal = uswdsModal.toggleModal as unknown as EventListener

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
    // We use dialog-id instead of the id attribute because USWDS does some DOM manipulation and
    // moves the id to a wrapper element, so it no longer points to the dialog itself.
    const modalButtons = document.querySelectorAll(
      `a[href='#` + modal.getAttribute('dialog-id') + `']`,
    )
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
    if (!onlyShowOnceGroup) {
      return
    }

    const modalHasBeenShown = localStorage.getItem(onlyShowOnceGroup)
    if (!modalHasBeenShown) {
      return
    }

    const bypassUrl = modal.getAttribute('bypass-url')
    if (!bypassUrl) {
      return
    }

    // Find corresponding buttons that open this modal and update href to bypassUrl
    const modalButtons = document.querySelectorAll<HTMLAnchorElement>(
      `a[href='#` + modal.getAttribute('dialog-id') + `']`,
    )
    modalButtons.forEach((modalButton) => {
      // This looks odd, but USWDS adds event listeners on the elements that trigger modal
      // dialogs, so we need to clone the element so that we can effectively update the
      // button to go straight through to the bypass URL instead of opening the modal.
      const newModalButton = modalButton.cloneNode(true) as HTMLAnchorElement
      modalButton.replaceWith(newModalButton)
      newModalButton.href = bypassUrl
      newModalButton.removeAttribute('data-open-modal')
      newModalButton.removeAttribute('aria-controls')
    })
  }

  /**
   * Makes the external program modals that HTMX rendered after page load work
   * like the ones USWDS set up at load.
   *
   * USWDS only sets up modals and triggers that exist when it initializes. It
   * moves each modal into a wrapper under <body> and gives the wrapper the
   * modal's id. When HTMX swaps in a program card, the card brings along a new,
   * unwrapped copy of its modal with the same id. Opening a wrapper without
   * USWDS's toggleModal would skip the focus trap, scroll lock and page hiding,
   * so keyboard focus could leave the modal.
   *
   * For a program USWDS already wrapped, this drops the duplicate modal so each
   * id is on the page once, and sends the new trigger to USWDS's own
   * toggleModal. For a program it hasn't seen, it asks USWDS to set up the new
   * modal and its triggers.
   */
  static setUpHTMXModals() {
    const triggers = document.querySelectorAll<HTMLElement>(
      'a[data-open-modal][href*="external-program-modal"]',
    )

    triggers.forEach((trigger) => {
      const modalId = trigger.getAttribute('aria-controls')
      if (!modalId) {
        return
      }

      const modalsWithId = Array.from(
        document.querySelectorAll(`[id="${CSS.escape(modalId)}"]`),
      )
      const wrapper = modalsWithId.find((element) =>
        element.classList.contains('usa-modal-wrapper'),
      )
      const unwrappedModals = modalsWithId.filter(
        (element) => element !== wrapper,
      )

      if (wrapper) {
        unwrappedModals.forEach((modal) =>
          (modal.closest('[id^="modal-container-"]') ?? modal).remove(),
        )
        trigger.addEventListener('click', preventDefaultNavigation)
        trigger.addEventListener('click', toggleUswdsModal)
      } else {
        unwrappedModals.forEach((modal) =>
          uswdsModal.on(
            modal.closest<HTMLElement>('[id^="modal-container-"]') ??
              (modal as HTMLElement),
          ),
        )
      }
    })
  }

  constructor() {
    const modals = document.querySelectorAll('.cf-ns-modal')

    modals.forEach((modal) => {
      const modalElement = modal as HTMLElement
      NorthStarModalController.attachModalListener(modalElement)

      NorthStarModalController.maybeUpdateButtonHrefToBypassUrl(modalElement)

      // To show a modal on load, we click the button linking to it so the modal is opened.
      if (modal.getAttribute('show-on-load')) {
        const modalButton = document.querySelector<HTMLAnchorElement>(
          `a[href='#` + modal.getAttribute('dialog-id') + `']`,
        )
        modalButton?.click()
      }
    })

    // Modals and triggers that HTMX adds after load aren't set up by USWDS.
    document.body.addEventListener('htmx:afterSwap', () => {
      NorthStarModalController.setUpHTMXModals()
    })

    // Advertise (e.g., for browser tests) that modal.ts initialization is done
    document.body.dataset.loadModal = 'true'
  }
}

window.onload = function () {
  // Maps buttons to elements that trigger modals
  const modals: Map<string, string> = new Map([
    ['invisible-validation-modal-button', 'show-error-modal'],
    [
      'invisible-duplicate-submission-modal-button',
      'show-duplicate-submission-modal',
    ],
  ])

  modals.forEach((value, key) => {
    const button = document.getElementById(key) as HTMLAnchorElement
    const shouldClickButton = document.getElementById(value)
    if (button && shouldClickButton) {
      button.click()
    }
  })
}

export function init() {
  new NorthStarModalController()
}
