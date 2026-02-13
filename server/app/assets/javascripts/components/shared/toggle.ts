/**
 * <wc-toggle> — Accessible toggle switch web component.
 *
 * Implements WAI-ARIA switch pattern:
 *   https://www.w3.org/WAI/ARIA/apg/patterns/switch/
 *
 * Attributes:
 *   sync-with — name of a light-DOM <input> to sync value to (enables native form POST)
 *   checked   — boolean, whether the toggle is on
 *   label     — visible label text, also used as accessible name via aria-label
 *   disabled  — disables interaction
 *   hidden    — hides the component
 *
 * Properties:
 *   checked: boolean  — get/set toggle state
 *   value: string     — "true" or "false" (read-only)
 *
 * Methods:
 *   toggle() — flip state and dispatch 'change' event
 *
 * Events:
 *   change — fires on toggle, bubbles through shadow DOM (composed: true)
 *
 * CSS custom properties:
 *   --wc-toggle-on-bg         (default: #2563eb)
 *   --wc-toggle-off-bg        (default: #4b5563)
 *   --wc-toggle-nub-bg        (default: #fff)
 *   --wc-toggle-focus-ring    (default: #2563eb)
 *   --wc-toggle-label-color   (default: #000)
 *   --wc-toggle-width         (default: 3.5rem)
 *   --wc-toggle-height        (default: 2rem)
 *   --wc-toggle-nub-size      (default: 1.5rem)
 *   --wc-toggle-nub-offset    (default: 0.25rem)
 *
 * Usage:
 *   <wc-toggle sync-with="emailAlerts" label="Email alerts" checked></wc-toggle>
 *   <input type="hidden" name="emailAlerts" />
 */

const template = document.createElement('template')
template.innerHTML = `
  <style>
    :host {
      display: inline-block;
    }

    :host([hidden]) {
      display: none;
    }

    .toggle-button {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0;
      margin: 0;
      border: none;
      background: transparent;
      cursor: pointer;
      font: inherit;
      color: inherit;
      -webkit-tap-highlight-color: transparent;
    }

    :host([disabled]) .toggle-button {
      cursor: not-allowed;
      opacity: 0.4;
    }

    .toggle-track {
      position: relative;
      width: var(--wc-toggle-width, 3.5rem);
      height: var(--wc-toggle-height, 2rem);
      border-radius: 9999px;
      background-color: var(--wc-toggle-off-bg, #4b5563);
      transition: background-color 150ms ease-in-out;
      flex-shrink: 0;
    }

    :host([checked]) .toggle-track {
      background-color: var(--wc-toggle-on-bg, #2563eb);
    }

    .toggle-nub {
      position: absolute;
      top: var(--wc-toggle-nub-offset, 0.25rem);
      left: var(--wc-toggle-nub-offset, 0.25rem);
      width: var(--wc-toggle-nub-size, 1.5rem);
      height: var(--wc-toggle-nub-size, 1.5rem);
      border-radius: 9999px;
      background-color: var(--wc-toggle-nub-bg, #fff);
      transition: transform 150ms ease-in-out;
    }

    :host([checked]) .toggle-nub {
      /*
       * Default travel = width(3.5rem) - nub(1.5rem) - 2*offset(0.25rem) = 1.5rem.
       * If you override the size custom properties, override this too:
       *   --wc-toggle-nub-travel: <your value>;
       */
      transform: translateX(var(--wc-toggle-nub-travel, 1.5rem));
    }

    .toggle-label {
      color: var(--wc-toggle-label-color, #000);
      font-weight: normal;
      user-select: none;
    }

    .toggle-button:focus-visible .toggle-track {
      outline: 2px solid var(--wc-toggle-focus-ring, #2563eb);
      outline-offset: 2px;
    }

    @media (prefers-reduced-motion: reduce) {
      .toggle-track,
      .toggle-nub {
        transition-duration: 0ms;
      }
    }
  </style>

  <button class="toggle-button" type="button" role="switch" aria-checked="false">
    <span class="toggle-track">
      <span class="toggle-nub"></span>
    </span>
    <span class="toggle-label"></span>
  </button>
`

export class WcToggle extends HTMLElement {
  private _button: HTMLButtonElement
  private _labelEl: HTMLSpanElement

  static get observedAttributes() {
    return ['checked', 'sync-with', 'label', 'disabled']
  }

  constructor() {
    super()
    this.attachShadow({mode: 'open'}).appendChild(
      template.content.cloneNode(true),
    )

    this._button = this.shadowRoot!.querySelector('.toggle-button')!
    this._labelEl = this.shadowRoot!.querySelector('.toggle-label')!

    this._button.addEventListener('click', (e) => {
      e.preventDefault()
      this.toggle()
    })

    this._button.addEventListener('keydown', (e) => {
      if (e.key === ' ' || e.key === 'Enter') {
        e.preventDefault()
        this.toggle()
      }
    })
  }

  connectedCallback() {
    this._syncButton()
    this._syncLabel()
    this._syncLinkedInput()
  }

  attributeChangedCallback(name: string) {
    if (!this.isConnected) return
    switch (name) {
      case 'checked':
        this._syncButton()
        this._syncLinkedInput()
        break
      case 'label':
        this._syncLabel()
        break
      case 'disabled':
        this._button.disabled = this.hasAttribute('disabled')
        this._button.setAttribute(
          'aria-disabled',
          String(this.hasAttribute('disabled')),
        )
        break
      case 'sync-with':
        this._syncLinkedInput()
        break
    }
  }

  get checked(): boolean {
    return this.hasAttribute('checked')
  }

  set checked(val: boolean) {
    if (val) {
      this.setAttribute('checked', '')
    } else {
      this.removeAttribute('checked')
    }
  }

  get value(): string {
    return this.checked.toString()
  }

  /** Flip state and dispatch a change event. No-op when disabled. */
  toggle() {
    if (this.hasAttribute('disabled')) return
    this.checked = !this.checked
    this.dispatchEvent(new Event('change', {bubbles: true, composed: true}))
  }

  /**
   * Sync value to the light-DOM hidden input whose name matches
   * the `sync-with` attribute. Scoped to closest <form>, then document.
   */
  private _syncLinkedInput() {
    const name = this.getAttribute('sync-with')
    if (!name) return
    const root = this.closest('form') ?? document
    const input = root.querySelector(
      `input[name="${name}"]`,
    )
    if (!input) return
    input.value = this.value
    input.setAttribute('value', this.value)
  }

  private _syncButton() {
    this._button.setAttribute('aria-checked', String(this.checked))
  }

  private _syncLabel() {
    const text = this.getAttribute('label') ?? ''
    this._labelEl.textContent = text
    this._button.setAttribute('aria-label', text || 'Toggle')
  }
}

if (!customElements.get('wc-toggle')) {
  customElements.define('wc-toggle', WcToggle)
}

export function init() {}
