import DOMPurify from 'dompurify'
import {default as uswdsModal} from '@uswds/uswds/js/usa-modal'

type ActionButton = HTMLButtonElement | HTMLAnchorElement

// Define the event detail interface
export interface ModalActionSelectedEvent {
  action: string
}

// Declare the custom event
declare global {
  interface HTMLElementEventMap {
    'modal:action-selected': CustomEvent<ModalActionSelectedEvent>
  }
}

/**
 * Web component modal - This wraps UWSDS modal
 *
 * @example Add the modal
 * ```html
 * <!-- Add the modal -->
 * <wc-modal hidden id="example-modal-container-1" modal-id="example-modal-1" heading-text="Heading Text">
 *   <div slot="description">Description Text</div>
 *   <div slot="buttons">
 *     <button class="usa-button" data-action="ok">OK</button>
 *     <button class="usa-button" data-action="cancel">Cancel</button>
 *   </div>
 * </wc-modal>
 * ```
 *
 * @example Use a declarative HTML trigger
 * ```html
 * <button aria-controls="example-modal-1" class="usa-button" data-open-modal>Open modal</button>
 * ```
 *
 * @example Listen for modal button clicks
 * ```typescript
 * const modal = document.getElementById('example-modal-container-1') as Modal
 * modal.addEventListener('modal:action-selected', function (event: CustomEvent<ModalActionSelectedEvent>) {
 *   if (event.detail.action === 'ok') {
 *     // Handle action
 *   }
 * })
 * ```
 *
 * @example Programmatically opening and closing the modal
 * ```typescript
 * const modal = document.getElementById('example-modal-container-1') as Modal
 * modal.open()
 * modal.close()
 * modal.toggle()
 * ```
 */
export class Modal extends HTMLElement {
  private _initialized = false
  private _modalId: string | null = null

  connectedCallback(): void {
    if (this._initialized) {
      return
    }

    this._initialized = true

    this._modalId = this.getAttribute('modal-id')
    const headingText = this.getAttribute('heading-text')

    if (!this._modalId) {
      throw new Error('wc-modal: modal-id attribute is required')
    }

    if (!headingText) {
      throw new Error('wc-modal: heading-text attribute is required')
    }

    const descriptionSlot = this.querySelector('[slot="description"]')
    const descriptionContent = DOMPurify.sanitize(
      descriptionSlot?.innerHTML ?? '',
    )

    if (!descriptionSlot) {
      throw new Error('wc-modal: description slot is required')
    }

    const actionButtons = Array.from(
      this.querySelector('[slot="buttons"]')?.children ?? [],
    ) as ActionButton[]

    if (
      actionButtons.some(
        (child) =>
          !(
            child instanceof HTMLButtonElement ||
            child instanceof HTMLAnchorElement
          ),
      )
    ) {
      throw new Error(
        'wc-modal: slot="buttons". Invalid elements in buttons slot. Direct children must be <button> or <a> elements.',
      )
    }

    const buttonFooter = this._buildButtonFooter(actionButtons)

    const forceAction =
      this.getAttribute('force-action') === 'true' ? 'data-force-action' : ''

    const closeButton =
      forceAction !== ''
        ? ''
        : `
      <button type="button" class="usa-button usa-modal__close" aria-label="Close this window" data-close-modal>
        <svg class="usa-icon" aria-hidden="true" focusable="false" role="img">
          <use href="/assets/Images/uswds/sprite.svg#close"></use>
        </svg>
      </button>    
    `

    const modalSize = this.getAttribute('size')
    let modalSizeClass = ''

    if (modalSize === 'lg') {
      modalSizeClass = `usa-modal--${modalSize}`
    }

    this.innerHTML = DOMPurify.sanitize(
      `
      <div class="usa-modal ${modalSizeClass}" id="${this._modalId}" aria-labelledby="${this._modalId}-modal-heading" aria-describedby="${this._modalId}-modal-description" ${forceAction}>
        <div class="usa-modal__content">
          <div class="usa-modal__main">
            <h1 class="usa-modal__heading" id="${this._modalId}-modal-heading">${headingText}</h1>
            <div class="usa-prose">
              <p id="${this._modalId}-modal-description">${descriptionContent}</p>
            </div>
            ${buttonFooter}
          </div>
          ${closeButton}
        </div>
      </div>
    `,
      {
        ADD_TAGS: ['use'],
        ADD_ATTR: ['href'],
      },
    )

    this._attachButtonListeners()
  }

  /**
   * Clean up when element is removed
   */
  disconnectedCallback(): void {
    if (!this._modalId) {
      return
    }

    const usaModalEl = document
      .getElementById(this._modalId)
      ?.querySelector<HTMLElement>('.usa-modal')

    if (!usaModalEl) {
      return
    }

    uswdsModal.teardown(usaModalEl)
  }

  private _buildButtonFooter(actionButtons: ActionButton[]): string {
    if (actionButtons.length === 0) {
      return ''
    }

    const buttonsText = actionButtons
      .map((actionButton) => {
        // Auto add the USWDS data-close-modal attribute to button if not present
        if (!actionButton.hasAttribute('data-close-modal')) {
          actionButton.dataset.closeModal = ''
        }

        if (
          !actionButton.hasAttribute('data-action') ||
          actionButton.dataset.action === ''
        ) {
          throw new Error(
            'wc-modal: slot="buttons". One or more button or anchor tags is missing a required data-action attribute.',
          )
        }

        return `<li class="usa-button-group__item">${actionButton.outerHTML}</li>`
      })
      .join('')

    return `
      <div class="usa-modal__footer">
        <ul class="usa-button-group">
          ${buttonsText}
        </ul>
      </div>    
    `
  }

  private _attachButtonListeners(): void {
    if (!this._modalId) {
      return
    }

    document
      .getElementById(this._modalId)
      ?.querySelectorAll('.usa-modal__footer button, .usa-modal__footer a')
      ?.forEach((element) => {
        element.removeEventListener('click', this._handleButtonClick)
        element.addEventListener('click', this._handleButtonClick)
      })
  }

  private _handleButtonClick = (event: Event) => {
    const element = event.currentTarget as HTMLElement

    this.dispatchEvent(
      new CustomEvent<ModalActionSelectedEvent>('modal:action-selected', {
        detail: {
          action: element.dataset.action as string,
        },
        bubbles: true,
        composed: true,
      }),
    )
  }

  /**
   * Opens this specific modal
   */
  open(): void {
    if (!this._modalId) {
      return
    }

    const wrapper = document.getElementById(this._modalId)

    // Verify the found _modalId is the correct class and is not already shown. These
    // are properties automatically added by USWDS.
    if (
      !wrapper ||
      !wrapper.classList.contains('usa-modal-wrapper') ||
      wrapper.classList.contains('is-visible')
    ) {
      return
    }

    // The fake target and event are used to simulate the expected parameters
    // when calling the USWDS toggleModal method. This allow for programmatic
    // calls to open the modal at times when using a declarative HTML trigger
    // is insufficient.
    const fakeTarget = {
      getAttribute: (attr: string) =>
        attr === 'aria-controls' ? this._modalId : null,
      setAttribute: () => {},
      hasAttribute: (attr: string) => attr === 'data-open-modal',
      closest: () => null,
    } as Partial<HTMLElement> as HTMLElement

    const fakeEvent = {
      target: fakeTarget,
      type: 'click',
    }

    uswdsModal.toggleModal.call(fakeTarget, fakeEvent)
  }

  /**
   * Closes this specific modal
   */
  close(): void {
    if (!this._modalId) {
      return
    }

    const wrapper = document.getElementById(this._modalId)

    // Verify the found _modalId is the correct class and is not already hidden. These
    // are properties automatically added by USWDS.
    if (
      !wrapper ||
      !wrapper.classList.contains('usa-modal-wrapper') ||
      !wrapper.classList.contains('is-visible')
    ) {
      return
    }

    // The fake target and event are used to simulate the expected parameters
    // when calling the USWDS toggleModal method. This allow for programmatic
    // calls to open the modal at times when using a declarative HTML trigger
    // is insufficient.
    const fakeTarget = {
      getAttribute: (attr: string) =>
        attr === 'aria-controls' ? this._modalId : null,
      setAttribute: () => {},
      hasAttribute: (attr: string) => attr === 'data-close-modal',
      closest: (selector: string) =>
        selector === '.usa-modal' ? wrapper.querySelector('.usa-modal') : null,
    } as Partial<HTMLElement> as HTMLElement

    const fakeEvent = {
      target: fakeTarget,
      type: 'click',
    }

    uswdsModal.toggleModal.call(fakeTarget, fakeEvent)
  }

  /**
   * Returns true when this modal is currently open
   */
  isOpen(): boolean {
    if (!this._modalId) {
      return false
    }

    return (
      document
        .getElementById(this._modalId)
        ?.classList.contains('is-visible') ?? false
    )
  }

  /**
   * Toggles this modal open/closed
   */
  toggle(): void {
    if (this.isOpen()) {
      this.close()
    } else {
      this.open()
    }
  }
}

customElements.define('wc-modal', Modal)
