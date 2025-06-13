/**
 * Toggles the visibility of the modal
 * @param {Element} modalWrapper The wrapper holding the modal
 * @param {boolean} showModal Whether the modal should be visible
 **/
function toggleModalVisibility(modalWrapper: Element, showModal: boolean) {
  modalWrapper.classList.toggle('is-visible', showModal)
  modalWrapper.classList.toggle('is-hidden', !showModal)
}

/**
 * Shows the modal
 * @param {Element} modalWrapper The wrapper holding the modal
 **/
function showModal(modalWrapper: Element) {
  toggleModalVisibility(modalWrapper, true)
}

/**
 * Hides the modal
 * @param {Element} modalWrapper The wrapper holding the modal
 * @param {HTMLElement} modalTrigger The button that triggered the modal
 **/
function hideModal(modalWrapper: Element, triggerButton: HTMLElement) {
  toggleModalVisibility(modalWrapper, false)
  // Return focus to the button that opened the modal
  triggerButton.focus()
}

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
   * Attaches an event listener to a button element rendered by HTMX after
   * initialization that triggers a USWDS modal dialog
   *
   * This method configures the button to show the modal when clicked, and to
   * hide the modal when a close action is triggered.
   *
   * @param {HTMLElement} triggerButton  The button element that will trigger
   *                                     the modal. Must have an 'aria-controls'
   *                                     attribute containing the ID of the
   *                                     target modal.
   */
  static attachHTMXModalListener(triggerButton: HTMLElement) {
    triggerButton.addEventListener('click', (e) => {
      // Default anchor navigation would focus on the href target. However, the
      // modal we want to display is a wrapper element added by USWDS and not
      // the one directly referenced by the href. Therefore, we need to remove
      // the default behavior and manually focus on the modal element (in a
      // later step).
      e.preventDefault()

      // Get the target modal ID
      const modalId = triggerButton.getAttribute('aria-controls')
      if (!modalId) {
        return
      }

      // Find the modal wrapper added to the DOM by the USWDS package
      const modalWrapper = document.querySelector(
        `.usa-modal-wrapper#${modalId}`,
      )
      if (!modalWrapper) {
        return
      }

      showModal(modalWrapper)

      // Focus on an element in the modal wrapper when it opens
      const openFocusEl: HTMLElement | null = modalWrapper.querySelector(
        `usa-modal *[data-focus]`,
      )
        ? modalWrapper.querySelector(`.usa-modal *[data-focus]`)
        : modalWrapper.querySelector(`.usa-modal`)
      if (openFocusEl) {
        openFocusEl.focus()
      }

      // Add listener to hide modal when a close attribute is clicked
      const closeButtons = modalWrapper.querySelectorAll('[data-close-modal]')
      closeButtons.forEach((closeButton) => {
        closeButton.addEventListener('click', () => {
          hideModal(modalWrapper, triggerButton)
        })
      })

      // Add listener to hide modal when the escape key is pressed
      const escapeKeyHandler = (event: KeyboardEvent) => {
        if (event.key === 'Escape') {
          hideModal(modalWrapper, triggerButton)
          // Remove listener after being triggered
          document.removeEventListener('keydown', escapeKeyHandler)
        }
      }
      document.addEventListener('keydown', escapeKeyHandler)

      // Add listener to hide modal when clicking outside the modal
      modalWrapper.addEventListener('click', (event) => {
        // Find the actual modal dialog within the wrapper
        const modalDialog = modalWrapper.querySelector('.usa-modal')

        // Hide modal if the click was outside the modal dialog
        if (modalDialog && !modalDialog.contains(event.target as Node)) {
          hideModal(modalWrapper, triggerButton)
        }
      })
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

    // USWDS modal trigger buttons added dynamically by HTMX after initial page
    // load don't automatically receive the event listeners that USWDS normally
    // attaches during its initialization. This is because USWDS only processes
    // elements that exist when it initializes.
    //
    // This event listener detects when HTMX adds new content to the page and
    // finds any external program modal triggers that were added. We then
    // manually attach our custom modal event listeners to these buttons to
    // ensure they can properly open their target modals.
    document.body.addEventListener('htmx:afterSwap', () => {
      const modalTrigger = document.querySelectorAll(
        'a[data-open-modal][href*="external-program-modal"]',
      )

      modalTrigger.forEach((trigger) => {
        const triggerElement = trigger as HTMLElement
        NorthStarModalController.attachHTMXModalListener(triggerElement)
      })
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
