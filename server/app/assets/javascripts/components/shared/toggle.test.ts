/**
 * Unit tests for <wc-toggle> web component (v2)
 *
 * Environment: vitest + jsdom
 *
 * vitest.config.ts:
 *   export default { test: { environment: 'jsdom' } }
 *
 * Key v2 changes reflected here:
 *   - No internal hidden <input>; sync-with targets a light-DOM <input>
 *   - attributeChangedCallback is gated on isConnected
 *   - aria-disabled added alongside button.disabled
 *   - No name property on the element
 *   - shadowRoot is real (open mode), queried via el.shadowRoot
 */

import {describe, it, expect, beforeEach, vi, afterEach} from 'vitest'
import {WcToggle} from '@/components/shared/toggle' // adjust path as needed

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Mount a <wc-toggle> in document.body, triggering connectedCallback. */
function makeToggle(attrs: Record<string, string | true> = {}): WcToggle {
  const el = document.createElement('wc-toggle') as WcToggle
  for (const [k, v] of Object.entries(attrs)) {
    el.setAttribute(k, v === true ? '' : v)
  }
  document.body.appendChild(el)
  return el
}

/** Query inside the open shadow root. */
function q<T extends Element = Element>(el: WcToggle, selector: string): T {
  return el.shadowRoot!.querySelector<T>(selector)!
}

const button = (el: WcToggle) => q<HTMLButtonElement>(el, '.toggle-button')
const labelEl = (el: WcToggle) => q<HTMLSpanElement>(el, '.toggle-label')

/** Dispatch a keydown on the shadow button. */
function pressKey(el: WcToggle, key: string) {
  button(el).dispatchEvent(
    new KeyboardEvent('keydown', {key, bubbles: true, cancelable: true}),
  )
}

// ---------------------------------------------------------------------------
// Suite
// ---------------------------------------------------------------------------

describe('<wc-toggle>', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ── Registration ───────────────────────────────────────────────────────────

  describe('custom element registration', () => {
    it('is registered as "wc-toggle"', () => {
      expect(customElements.get('wc-toggle')).toBe(WcToggle)
    })

    it('document.createElement returns a WcToggle instance', () => {
      expect(document.createElement('wc-toggle')).toBeInstanceOf(WcToggle)
    })
  })

  // ── Shadow DOM structure ───────────────────────────────────────────────────

  describe('shadow DOM structure', () => {
    it('attaches an open shadow root', () => {
      const el = makeToggle()
      expect(el.shadowRoot).not.toBeNull()
    })

    it('contains a button with role="switch"', () => {
      const el = makeToggle()
      expect(button(el).getAttribute('role')).toBe('switch')
      expect(button(el).type).toBe('button')
    })

    it('contains a .toggle-label span', () => {
      const el = makeToggle()
      expect(labelEl(el)).not.toBeNull()
    })

    it('does NOT contain an internal hidden input (sync-with replaces it)', () => {
      const el = makeToggle()
      expect(el.shadowRoot!.querySelector('input')).toBeNull()
    })
  })

  // ── Initial state ──────────────────────────────────────────────────────────

  describe('initial state', () => {
    it('is unchecked by default', () => {
      expect(makeToggle().checked).toBe(false)
    })

    it('is checked when the checked attribute is present at mount', () => {
      expect(makeToggle({checked: true}).checked).toBe(true)
    })

    it('aria-checked is "false" by default', () => {
      expect(button(makeToggle()).getAttribute('aria-checked')).toBe('false')
    })

    it('aria-checked is "true" when checked attribute is set at mount', () => {
      expect(
        button(makeToggle({checked: true})).getAttribute('aria-checked'),
      ).toBe('true')
    })
  })

  // ── checked property ───────────────────────────────────────────────────────

  describe('checked property', () => {
    it('setter with true adds the checked attribute', () => {
      const el = makeToggle()
      el.checked = true
      expect(el.hasAttribute('checked')).toBe(true)
    })

    it('setter with false removes the checked attribute', () => {
      const el = makeToggle({checked: true})
      el.checked = false
      expect(el.hasAttribute('checked')).toBe(false)
    })

    it('getter reflects the attribute correctly', () => {
      const el = makeToggle()
      el.setAttribute('checked', '')
      expect(el.checked).toBe(true)
      el.removeAttribute('checked')
      expect(el.checked).toBe(false)
    })
  })

  // ── value property ─────────────────────────────────────────────────────────

  describe('value property', () => {
    it('returns "false" when unchecked', () => {
      expect(makeToggle().value).toBe('false')
    })

    it('returns "true" when checked', () => {
      expect(makeToggle({checked: true}).value).toBe('true')
    })

    it('updates after toggle()', () => {
      const el = makeToggle()
      el.toggle()
      expect(el.value).toBe('true')
    })
  })

  // ── label attribute ────────────────────────────────────────────────────────

  describe('label attribute', () => {
    it('sets label text content', () => {
      const el = makeToggle({label: 'Email alerts'})
      expect(labelEl(el).textContent).toBe('Email alerts')
    })

    it('sets aria-label to the label text', () => {
      const el = makeToggle({label: 'Email alerts'})
      expect(button(el).getAttribute('aria-label')).toBe('Email alerts')
    })

    it('falls back to "Toggle" for aria-label when label is absent', () => {
      expect(button(makeToggle()).getAttribute('aria-label')).toBe('Toggle')
    })

    it('updates live when label attribute changes after mount', () => {
      const el = makeToggle({label: 'Old label'})
      el.setAttribute('label', 'New label')
      expect(labelEl(el).textContent).toBe('New label')
      expect(button(el).getAttribute('aria-label')).toBe('New label')
    })
  })

  // ── disabled attribute ─────────────────────────────────────────────────────

  describe('disabled attribute', () => {
    it('button.disabled is true when disabled attribute is set at mount', () => {
      expect(button(makeToggle({disabled: true})).disabled).toBe(true)
    })

    it('sets aria-disabled="true" when disabled at mount', () => {
      expect(
        button(makeToggle({disabled: true})).getAttribute('aria-disabled'),
      ).toBe('true')
    })

    it('button.disabled becomes false when disabled attribute is removed', () => {
      const el = makeToggle({disabled: true})
      el.removeAttribute('disabled')
      expect(button(el).disabled).toBe(false)
    })

    it('aria-disabled becomes "false" when disabled attribute is removed', () => {
      const el = makeToggle({disabled: true})
      el.removeAttribute('disabled')
      expect(button(el).getAttribute('aria-disabled')).toBe('false')
    })
  })

  // ── toggle() method ────────────────────────────────────────────────────────

  describe('toggle()', () => {
    it('turns on when off', () => {
      const el = makeToggle()
      el.toggle()
      expect(el.checked).toBe(true)
    })

    it('turns off when on', () => {
      const el = makeToggle({checked: true})
      el.toggle()
      expect(el.checked).toBe(false)
    })

    it('updates aria-checked after toggling on', () => {
      const el = makeToggle()
      el.toggle()
      expect(button(el).getAttribute('aria-checked')).toBe('true')
    })

    it('updates aria-checked after toggling off', () => {
      const el = makeToggle({checked: true})
      el.toggle()
      expect(button(el).getAttribute('aria-checked')).toBe('false')
    })

    it('is a no-op when disabled', () => {
      const el = makeToggle({disabled: true})
      el.toggle()
      expect(el.checked).toBe(false)
    })

    it('dispatches a "change" event', () => {
      const el = makeToggle()
      const listener = vi.fn()
      el.addEventListener('change', listener)
      el.toggle()
      expect(listener).toHaveBeenCalledTimes(1)
    })

    it('"change" event bubbles', () => {
      const el = makeToggle()
      const listener = vi.fn()
      document.body.addEventListener('change', listener)
      el.toggle()
      expect(listener).toHaveBeenCalledTimes(1)
    })

    it('"change" event is composed (crosses shadow boundary)', () => {
      const el = makeToggle()
      let composed = false
      el.addEventListener('change', (e) => {
        composed = (e).composed
      })
      el.toggle()
      expect(composed).toBe(true)
    })

    it('does not dispatch "change" when disabled', () => {
      const el = makeToggle({disabled: true})
      const listener = vi.fn()
      el.addEventListener('change', listener)
      el.toggle()
      expect(listener).not.toHaveBeenCalled()
    })
  })

  // ── click interaction ──────────────────────────────────────────────────────

  describe('click interaction', () => {
    it('clicking the button toggles state on', () => {
      const el = makeToggle()
      button(el).click()
      expect(el.checked).toBe(true)
    })

    it('clicking twice returns to the original state', () => {
      const el = makeToggle()
      button(el).click()
      button(el).click()
      expect(el.checked).toBe(false)
    })

    it('dispatches a change event on click', () => {
      const el = makeToggle()
      const listener = vi.fn()
      el.addEventListener('change', listener)
      button(el).click()
      expect(listener).toHaveBeenCalledTimes(1)
    })
  })

  // ── keyboard interaction ───────────────────────────────────────────────────

  describe('keyboard interaction', () => {
    it('Space key toggles state on', () => {
      const el = makeToggle()
      pressKey(el, ' ')
      expect(el.checked).toBe(true)
    })

    it('Enter key toggles state on', () => {
      const el = makeToggle()
      pressKey(el, 'Enter')
      expect(el.checked).toBe(true)
    })

    it('other keys (e.g. Tab) do not toggle', () => {
      const el = makeToggle()
      pressKey(el, 'Tab')
      expect(el.checked).toBe(false)
    })

    it('Space key on a disabled toggle does nothing', () => {
      const el = makeToggle({disabled: true})
      pressKey(el, ' ')
      expect(el.checked).toBe(false)
    })

    it('Enter key on a disabled toggle does nothing', () => {
      const el = makeToggle({disabled: true})
      pressKey(el, 'Enter')
      expect(el.checked).toBe(false)
    })
  })

  // ── sync-with attribute ────────────────────────────────────────────────────

  describe('sync-with attribute', () => {
    /** Add a light-DOM hidden input and a toggle that syncs to it. */
    function setupWithInput(
      name: string,
      toggleAttrs: Record<string, string | true> = {},
    ) {
      const input = document.createElement('input')
      input.type = 'hidden'
      input.name = name
      document.body.appendChild(input)
      const el = makeToggle({'sync-with': name, ...toggleAttrs})
      return {el, input}
    }

    it('syncs linked input value to "false" on connect when unchecked', () => {
      const {input} = setupWithInput('myField')
      expect(input.value).toBe('false')
    })

    it('syncs linked input value to "true" on connect when checked', () => {
      const {input} = setupWithInput('myField', {checked: true})
      expect(input.value).toBe('true')
    })

    it('updates linked input value after toggle()', () => {
      const {el, input} = setupWithInput('myField')
      el.toggle()
      expect(input.value).toBe('true')
    })

    it('updates linked input value after setting checked property', () => {
      const {el, input} = setupWithInput('myField')
      el.checked = true
      expect(input.value).toBe('true')
    })

    it('sets the "value" attribute on the linked input (for native form POST)', () => {
      const {el, input} = setupWithInput('myField')
      el.toggle()
      expect(input.getAttribute('value')).toBe('true')
    })

    it('does not throw when no matching input exists', () => {
      expect(() => makeToggle({'sync-with': 'nonexistent'})).not.toThrow()
    })

    it('scopes input lookup to closest <form>, ignoring same-name inputs outside it', () => {
      const form = document.createElement('form')
      document.body.appendChild(form)

      const innerInput = document.createElement('input')
      innerInput.type = 'hidden'
      innerInput.name = 'field'
      form.appendChild(innerInput)

      const outerInput = document.createElement('input')
      outerInput.type = 'hidden'
      outerInput.name = 'field'
      outerInput.value = 'untouched'
      document.body.appendChild(outerInput)

      const el = document.createElement('wc-toggle') as WcToggle
      el.setAttribute('sync-with', 'field')
      form.appendChild(el) // connectedCallback fires here

      el.toggle()
      expect(innerInput.value).toBe('true')
      expect(outerInput.value).toBe('untouched')
    })

    it('re-syncs when sync-with attribute changes to a different input name', () => {
      const inputA = document.createElement('input')
      inputA.type = 'hidden'
      inputA.name = 'fieldA'
      document.body.appendChild(inputA)

      const inputB = document.createElement('input')
      inputB.type = 'hidden'
      inputB.name = 'fieldB'
      inputB.value = 'untouched'
      document.body.appendChild(inputB)

      const el = makeToggle({'sync-with': 'fieldA', checked: true})
      expect(inputA.value).toBe('true')

      el.setAttribute('sync-with', 'fieldB')
      expect(inputB.value).toBe('true') // now synced to fieldB
    })
  })

  // ── isConnected guard in attributeChangedCallback ─────────────────────────

  describe('isConnected guard', () => {
    it('does not sync aria-checked while the element is disconnected', () => {
      // Create but do NOT append to DOM
      const el = document.createElement('wc-toggle') as WcToggle
      el.setAttribute('checked', '') // attributeChangedCallback fires, but isConnected=false
      expect(button(el).getAttribute('aria-checked')).toBe('false') // still constructor default
    })

    it('syncs aria-checked once connected to the DOM', () => {
      const el = document.createElement('wc-toggle') as WcToggle
      el.setAttribute('checked', '')
      document.body.appendChild(el) // connectedCallback runs _syncButton
      expect(button(el).getAttribute('aria-checked')).toBe('true')
    })

    it('syncs label once connected to the DOM', () => {
      const el = document.createElement('wc-toggle') as WcToggle
      el.setAttribute('label', 'My label')
      // Still disconnected — label span should be empty
      expect(labelEl(el).textContent).toBe('')
      document.body.appendChild(el)
      expect(labelEl(el).textContent).toBe('My label')
    })
  })

  // ── hidden attribute (CSS behaviour note) ─────────────────────────────────

  describe('hidden attribute', () => {
    it('the hidden attribute is present when set (CSS :host([hidden]) handles display:none)', () => {
      // jsdom does not compute Shadow DOM styles, so we verify the attribute
      // is forwarded correctly for the CSS selector to match in a real browser.
      const el = makeToggle({hidden: true})
      expect(el.hasAttribute('hidden')).toBe(true)
    })
  })
})
