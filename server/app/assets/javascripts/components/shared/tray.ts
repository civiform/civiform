/**
 * Tray Web Component
 * A modal tray/drawer that slides in from any edge of the screen
 *
 * Usage: <wc-tray direction="left" heading="Navigation" aria-label="Navigation menu">...</wc-tray>
 *
 * Attributes:
 * - direction: "left" | "right" | "top" | "bottom" (default: "right")
 * - heading: Text for the h1 heading displayed in the tray header
 * - open: boolean attribute to control open state
 * - aria-label: Label for the tray (required for accessibility)
 *
 * Methods:
 * - show(): Opens the tray
 * - hide(): Closes the tray
 * - toggle(): Toggles the tray state
 *
 * Events:
 * - tray-open: Fired when the tray opens
 * - tray-close: Fired when the tray closes
 *
 * CSS Parts:
 * - backdrop: The dimmed overlay
 * - tray: The sliding panel
 * - header: The header container with heading and close button
 * - heading: The h1 heading element
 * - close-button: The close button
 * - content: The content wrapper
 *
 * Accessibility Note:
 * The h1 inside the tray is scoped to the dialog context via role="dialog" and aria-modal="true".
 * Screen readers treat modal dialogs as separate document contexts, so the h1 here does not
 * conflict with or disrupt the main page's heading hierarchy. This follows WAI-ARIA best practices
 * for modal dialogs where the dialog title should be a prominent heading within the dialog.
 */

export type TrayDirection = 'left' | 'right' | 'top' | 'bottom'

export interface TrayEventMap {
  'tray-open': CustomEvent<void>
  'tray-close': CustomEvent<void>
}

export class Tray extends HTMLElement {
  static get observedAttributes(): string[] {
    return ['direction', 'open', 'heading']
  }

  private _isOpen: boolean = false
  private _previouslyFocusedElement: HTMLElement | null = null
  private _focusableElements: HTMLElement[] = []
  private _sheet: CSSStyleSheet | null = null

  // Bound event handlers for cleanup
  private _handleBackdropClick: (() => void) | null = null
  private _handleCloseClick: (() => void) | null = null
  private _handleKeydown: ((e: KeyboardEvent) => void) | null = null

  constructor() {
    super()
    this.attachShadow({mode: 'open'})
    this._sheet = new CSSStyleSheet()
  }

  connectedCallback(): void {
    this.render()
    this.setupEventListeners()

    // Check if should be open initially
    if (this.hasAttribute('open')) {
      this.show()
    }
  }

  disconnectedCallback(): void {
    this.removeEventListeners()
  }

  attributeChangedCallback(
    name: string,
    oldValue: string | null,
    newValue: string | null,
  ): void {
    if (name === 'direction' && this.shadowRoot) {
      this.updateStyles()
    }
    if (name === 'heading' && this.shadowRoot) {
      const headingEl = this.shadowRoot.querySelector('.tray-heading')
      if (headingEl) {
        headingEl.textContent = newValue || ''
      }
    }
    if (name === 'open') {
      if (newValue !== null) {
        this.show()
      } else {
        this.hide()
      }
    }
  }

  get direction(): TrayDirection {
    const dir = this.getAttribute('direction')
    if (
      dir === 'left' ||
      dir === 'right' ||
      dir === 'top' ||
      dir === 'bottom'
    ) {
      return dir
    }
    return 'right'
  }

  set direction(value: TrayDirection) {
    this.setAttribute('direction', value)
  }

  get heading(): string {
    return this.getAttribute('heading') || ''
  }

  set heading(value: string) {
    this.setAttribute('heading', value)
  }

  get isOpen(): boolean {
    return this._isOpen
  }

  private getStyles(): string {
    const direction = this.direction

    // Determine transform and positioning based on direction
    const transforms: Record<TrayDirection, string> = {
      left: 'translateX(-100%)',
      right: 'translateX(100%)',
      top: 'translateY(-100%)',
      bottom: 'translateY(100%)',
    }

    const positions: Record<TrayDirection, string> = {
      left: 'left: 0; top: 0; bottom: 0; width: 50vw; height: 100%;',
      right: 'right: 0; top: 0; bottom: 0; width: 50vw; height: 100%;',
      top: 'top: 0; left: 0; right: 0; height: min(320px, 60vh); width: 100%;',
      bottom:
        'bottom: 0; left: 0; right: 0; height: min(320px, 60vh); width: 100%;',
    }

    return `
      :host {
        display: contents;
      }

      .backdrop {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.5);
        opacity: 0;
        visibility: hidden;
        transition: opacity 0.3s ease, visibility 0.3s ease;
        z-index: 9998;
      }

      .backdrop.visible {
        opacity: 1;
        visibility: visible;
      }

      .tray {
        position: fixed;
        ${positions[direction]}
        background: white;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
        transform: ${transforms[direction]};
        transition: transform 0.3s ease;
        z-index: 9999;
        overflow-y: auto;
        overflow-x: hidden;
        display: flex;
        flex-direction: column;
      }

      .tray.open {
        transform: translate(0, 0);
      }

      .tray-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.75rem 1rem;
        border-bottom: 1px solid #e5e7eb;
        flex-shrink: 0;
      }

      .tray-heading {
        margin: 0;
        font-size: 1.25rem;
        font-weight: 600;
        color: #111827;
      }

      .close-button {
        width: 2.5rem;
        height: 2.5rem;
        border: none;
        background: transparent;
        cursor: pointer;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;
        flex-shrink: 0;
      }

      .close-button:hover {
        background: rgba(0, 0, 0, 0.1);
      }

      .close-button:focus-visible {
        outline: 2px solid #3b82f6;
        outline-offset: 2px;
      }

      .close-button svg {
        width: 1.25rem;
        height: 1.25rem;
        color: #374151;
      }

      .content {
        flex: 1;
        overflow-y: auto;
        box-sizing: border-box;
        padding: 1rem;
      }

      @media (prefers-reduced-motion: reduce) {
        .backdrop,
        .tray {
          transition: none;
        }
      }
    `
  }

  private updateStyles(): void {
    if (this._sheet) {
      this._sheet.replaceSync(this.getStyles())
    }
  }

  private render(): void {
    if (!this.shadowRoot || !this._sheet) return

    // Apply styles via adopted stylesheets
    this._sheet.replaceSync(this.getStyles())
    this.shadowRoot.adoptedStyleSheets = [this._sheet]

    // Create HTML using template
    const headingId = `tray-heading-${Math.random().toString(36).substr(2, 9)}`
    const template = document.createElement('template')
    template.innerHTML = `
      <div class="backdrop" part="backdrop"></div>
      <div class="tray" part="tray" role="dialog" aria-modal="true" aria-labelledby="${headingId}">
        <header class="tray-header" part="header">
          <h1 id="${headingId}" class="tray-heading" part="heading">${this.heading}</h1>
          <button class="close-button" aria-label="Close" part="close-button">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </header>
        <div class="content" part="content">
          <slot></slot>
        </div>
      </div>
    `

    // Clear and append template content
    this.shadowRoot.innerHTML = ''
    this.shadowRoot.appendChild(template.content.cloneNode(true))

    // Re-attach event listeners after re-render
    this.setupEventListeners()

    // Restore open state if needed
    if (this._isOpen) {
      const backdrop = this.shadowRoot.querySelector('.backdrop')
      const tray = this.shadowRoot.querySelector('.tray')
      backdrop?.classList.add('visible')
      tray?.classList.add('open')
    }
  }

  private setupEventListeners(): void {
    if (!this.shadowRoot) return

    const backdrop = this.shadowRoot.querySelector('.backdrop')
    const closeButton = this.shadowRoot.querySelector('.close-button')

    // Remove old listeners first
    this.removeEventListeners()

    // Store bound handlers for removal
    this._handleBackdropClick = () => this.hide()
    this._handleCloseClick = () => this.hide()
    this._handleKeydown = (e: KeyboardEvent) => this.handleKeydown(e)

    backdrop?.addEventListener('click', this._handleBackdropClick)
    closeButton?.addEventListener('click', this._handleCloseClick)
    document.addEventListener('keydown', this._handleKeydown)
  }

  private removeEventListeners(): void {
    if (!this.shadowRoot) return

    const backdrop = this.shadowRoot.querySelector('.backdrop')
    const closeButton = this.shadowRoot.querySelector('.close-button')

    if (this._handleBackdropClick) {
      backdrop?.removeEventListener('click', this._handleBackdropClick)
    }
    if (this._handleCloseClick) {
      closeButton?.removeEventListener('click', this._handleCloseClick)
    }
    if (this._handleKeydown) {
      document.removeEventListener('keydown', this._handleKeydown)
    }
  }

  private handleKeydown(e: KeyboardEvent): void {
    if (!this._isOpen) return

    if (e.key === 'Escape') {
      e.preventDefault()
      this.hide()
      return
    }

    // Trap focus within the tray
    if (e.key === 'Tab') {
      this.trapFocus(e)
    }
  }

  private trapFocus(e: KeyboardEvent): void {
    if (!this.shadowRoot) return

    const tray = this.shadowRoot.querySelector('.tray')
    if (!tray) return

    // Get all focusable elements in shadow DOM and light DOM
    const focusableSelector =
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'

    const shadowFocusable =
      tray.querySelectorAll<HTMLElement>(focusableSelector)
    const slotted = this.querySelectorAll<HTMLElement>(focusableSelector)

    this._focusableElements = [
      ...Array.from(shadowFocusable),
      ...Array.from(slotted),
    ].filter(
      (el): el is HTMLElement =>
        !(el as HTMLButtonElement).disabled && el.offsetParent !== null,
    )

    if (this._focusableElements.length === 0) return

    const firstElement = this._focusableElements[0]
    const lastElement =
      this._focusableElements[this._focusableElements.length - 1]

    if (e.shiftKey && document.activeElement === firstElement) {
      e.preventDefault()
      lastElement.focus()
    } else if (!e.shiftKey && document.activeElement === lastElement) {
      e.preventDefault()
      firstElement.focus()
    }
  }

  show(): void {
    if (this._isOpen) return
    if (!this.shadowRoot) return

    // Store the currently focused element
    this._previouslyFocusedElement =
      document.activeElement as HTMLElement | null

    // Prevent body scroll
    document.body.style.overflow = 'hidden'

    // Update state
    this._isOpen = true

    // Add open classes
    const backdrop = this.shadowRoot.querySelector('.backdrop')
    const tray = this.shadowRoot.querySelector('.tray')
    backdrop?.classList.add('visible')
    tray?.classList.add('open')

    // Announce to screen readers
    this.announceToScreenReader('Dialog opened')

    // Focus the close button after animation
    setTimeout(() => {
      const closeButton =
        this.shadowRoot?.querySelector<HTMLButtonElement>('.close-button')
      closeButton?.focus()
    }, 100)

    // Dispatch event
    this.dispatchEvent(
      new CustomEvent('tray-open', {
        bubbles: true,
        composed: true,
      }),
    )
  }

  hide(): void {
    if (!this._isOpen) return
    if (!this.shadowRoot) return

    // Restore body scroll
    document.body.style.overflow = ''

    // Update state
    this._isOpen = false

    // Remove open classes
    const backdrop = this.shadowRoot.querySelector('.backdrop')
    const tray = this.shadowRoot.querySelector('.tray')
    backdrop?.classList.remove('visible')
    tray?.classList.remove('open')

    // Announce to screen readers
    this.announceToScreenReader('Dialog closed')

    // Restore focus to previously focused element
    if (this._previouslyFocusedElement) {
      this._previouslyFocusedElement.focus()
      this._previouslyFocusedElement = null
    }

    // Dispatch event
    this.dispatchEvent(
      new CustomEvent('tray-close', {
        bubbles: true,
        composed: true,
      }),
    )
  }

  toggle(): void {
    if (this._isOpen) {
      this.hide()
    } else {
      this.show()
    }
  }

  private announceToScreenReader(message: string): void {
    // Create a live region for announcements
    let announcer = document.getElementById('wc-tray-announcer')
    if (!announcer) {
      announcer = document.createElement('div')
      announcer.id = 'wc-tray-announcer'
      announcer.setAttribute('aria-live', 'polite')
      announcer.setAttribute('aria-atomic', 'true')
      announcer.style.cssText =
        'position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0;'
      document.body.appendChild(announcer)
    }

    // Clear and set new message
    announcer.textContent = ''
    setTimeout(() => {
      announcer.textContent = message
    }, 50)
  }

  // Type-safe event listener methods
  addEventListener<K extends keyof TrayEventMap>(
    type: K,
    listener: (this: Tray, ev: TrayEventMap[K]) => void,
    options?: boolean | AddEventListenerOptions,
  ): void
  addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
    options?: boolean | AddEventListenerOptions,
  ): void {
    super.addEventListener(type, listener, options)
  }

  removeEventListener<K extends keyof TrayEventMap>(
    type: K,
    listener: (this: Tray, ev: TrayEventMap[K]) => void,
    options?: boolean | EventListenerOptions,
  ): void
  removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
    options?: boolean | EventListenerOptions,
  ): void {
    super.removeEventListener(type, listener, options)
  }
}

// Register the custom element
customElements.define('wc-tray', Tray)

// Declare the custom element for TypeScript
declare global {
  interface HTMLElementTagNameMap {
    'wc-tray': Tray
  }
}
