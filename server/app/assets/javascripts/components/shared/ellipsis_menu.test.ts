import {describe, it, expect, beforeEach, afterEach, vi} from 'vitest'
import './ellipsis_menu'

/**
 * Helper: create an external trigger button + <wc-ellipsis-menu> with the
 * given menu-link children, append both to the document.
 */
function createElement(
  links: Array<{href: string; label: string; variant?: string}>,
): {el: HTMLElement; trigger: HTMLButtonElement} {
  const triggerId = `trigger-${Math.random().toString(36).slice(2, 10)}`

  const trigger = document.createElement('button')
  trigger.id = triggerId
  trigger.textContent = '⋮'
  document.body.appendChild(trigger)

  const el = document.createElement('wc-ellipsis-menu')
  el.setAttribute('trigger', triggerId)
  for (const link of links) {
    const ml = document.createElement('menu-link')
    ml.setAttribute('href', link.href)
    ml.setAttribute('label', link.label)
    if (link.variant) ml.setAttribute('variant', link.variant)
    el.appendChild(ml)
  }
  document.body.appendChild(el)
  return {el, trigger}
}

function getMenu(el: HTMLElement): HTMLUListElement {
  // Menu is in the document body, find it via the trigger's popovertarget
  const trigger = document.getElementById(
    el.getAttribute('trigger')!,
  ) as HTMLButtonElement
  const menuId = trigger.getAttribute('popovertarget')!
  return document.getElementById(menuId) as HTMLUListElement
}

function getMenuItems(el: HTMLElement): HTMLAnchorElement[] {
  const menu = getMenu(el)
  return Array.from(
    menu.querySelectorAll<HTMLAnchorElement>('.ellipsis-menu-item a'),
  )
}

/**
 * Simulate the popover toggle event since jsdom/happy-dom
 * don't natively support the Popover API.
 */
function fireToggleEvent(menu: HTMLElement, newState: 'open' | 'closed'): void {
  const event = new Event('toggle') as Event & {newState?: string}
  Object.defineProperty(event, 'newState', {value: newState})
  menu.dispatchEvent(event)
}

function fireKeydown(target: HTMLElement, key: string): void {
  target.dispatchEvent(new KeyboardEvent('keydown', {key, bubbles: true}))
}

const sampleLinks = [
  {href: '/edit', label: 'Edit'},
  {href: '/duplicate', label: 'Duplicate'},
  {href: '/share', label: 'Share'},
]

describe('wc-ellipsis-menu', () => {
  let el: HTMLElement
  let trigger: HTMLButtonElement

  afterEach(() => {
    el?.remove() // disconnectedCallback cleans up the menu
    trigger?.remove()
  })

  // ──────────────────────────────────────────────
  // Rendering
  // ──────────────────────────────────────────────

  describe('rendering', () => {
    beforeEach(() => {
      ;({el, trigger} = createElement(sampleLinks))
    })

    it('does not render a trigger button inside the component', () => {
      const internalButton = el.querySelector('button')
      expect(internalButton).toBeNull()
    })

    it('renders the menu in the document body', () => {
      const menu = getMenu(el)
      expect(menu).not.toBeNull()
      expect(menu.parentElement).toBe(document.body)
    })

    it('renders the correct number of menu items', () => {
      const items = getMenuItems(el)
      expect(items).toHaveLength(3)
    })

    it('renders menu item labels correctly', () => {
      const items = getMenuItems(el)
      expect(items.map((a) => a.textContent)).toEqual([
        'Edit',
        'Duplicate',
        'Share',
      ])
    })

    it('renders menu item hrefs correctly', () => {
      const items = getMenuItems(el)
      expect(items.map((a) => a.getAttribute('href'))).toEqual([
        '/edit',
        '/duplicate',
        '/share',
      ])
    })

    it('renders with no links when none are provided', () => {
      el.remove()
      trigger.remove()
      ;({el, trigger} = createElement([]))
      const items = getMenuItems(el)
      expect(items).toHaveLength(0)
    })

    it('uses default href and label when attributes are missing', () => {
      el.remove()
      trigger.remove()

      const triggerId = `trigger-${Math.random().toString(36).slice(2, 10)}`
      const btn = document.createElement('button')
      btn.id = triggerId
      document.body.appendChild(btn)

      const custom = document.createElement('wc-ellipsis-menu')
      custom.setAttribute('trigger', triggerId)
      const ml = document.createElement('menu-link')
      // No href or label set
      custom.appendChild(ml)
      document.body.appendChild(custom)
      el = custom
      trigger = btn

      const items = getMenuItems(el)
      expect(items[0].getAttribute('href')).toBe('#')
      expect(items[0].textContent).toBe('Link')
    })
  })

  // ──────────────────────────────────────────────
  // External trigger wiring
  // ──────────────────────────────────────────────

  describe('external trigger wiring', () => {
    beforeEach(() => {
      ;({el, trigger} = createElement(sampleLinks))
    })

    it('sets popovertarget on the external trigger matching menu id', () => {
      const menu = getMenu(el)
      expect(trigger.getAttribute('popovertarget')).toBe(menu.id)
    })

    it('sets anchor-name on the external trigger', () => {
      // Using @ts-ignore because anchorName is showing as invalid. It exists, the typescript types are just behind
      // @ts-ignore
      expect(trigger.style.anchorName).toMatch(/^--menu-/)
    })

    it('sets aria-haspopup on the external trigger', () => {
      expect(trigger.getAttribute('aria-haspopup')).toBe('true')
    })

    it('sets aria-expanded="false" on the external trigger initially', () => {
      expect(trigger.getAttribute('aria-expanded')).toBe('false')
    })

    it('does not overwrite existing aria-haspopup on trigger', () => {
      el.remove()
      trigger.remove()

      const triggerId = `trigger-${Math.random().toString(36).slice(2, 10)}`
      const btn = document.createElement('button')
      btn.id = triggerId
      btn.setAttribute('aria-haspopup', 'dialog')
      document.body.appendChild(btn)

      const custom = document.createElement('wc-ellipsis-menu')
      custom.setAttribute('trigger', triggerId)
      document.body.appendChild(custom)
      el = custom
      trigger = btn

      expect(trigger.getAttribute('aria-haspopup')).toBe('dialog')
    })

    it('throws if trigger attribute is missing', () => {
      const bad = document.createElement('wc-ellipsis-menu')
      expect(() => document.body.appendChild(bad)).toThrow(
        'requires a "trigger" attribute',
      )
      bad.remove()
    })

    it('throws if trigger element is not found', () => {
      const bad = document.createElement('wc-ellipsis-menu')
      bad.setAttribute('trigger', 'nonexistent-id')
      expect(() => document.body.appendChild(bad)).toThrow(
        'no element found with id',
      )
      bad.remove()
    })
  })

  // ──────────────────────────────────────────────
  // Accessibility attributes
  // ──────────────────────────────────────────────

  describe('accessibility', () => {
    beforeEach(() => {
      ;({el, trigger} = createElement(sampleLinks))
    })

    it('menu has role="menu"', () => {
      expect(getMenu(el).getAttribute('role')).toBe('menu')
    })

    it('menu has aria-label', () => {
      expect(getMenu(el).getAttribute('aria-label')).toBe('Options')
    })

    it('menu has popover attribute', () => {
      expect(getMenu(el).hasAttribute('popover')).toBe(true)
    })

    it('menu items have role="menuitem"', () => {
      const items = getMenuItems(el)
      for (const item of items) {
        expect(item.getAttribute('role')).toBe('menuitem')
      }
    })

    it('menu items have tabindex="-1"', () => {
      const items = getMenuItems(el)
      for (const item of items) {
        expect(item.getAttribute('tabindex')).toBe('-1')
      }
    })

    it('menu item list items have role="none"', () => {
      const menu = getMenu(el)
      const lis = Array.from(
        menu.querySelectorAll<HTMLLIElement>('.ellipsis-menu-item'),
      )
      for (const li of lis) {
        expect(li.getAttribute('role')).toBe('none')
      }
    })
  })

  // ──────────────────────────────────────────────
  // Popover toggle (aria-expanded sync)
  // ──────────────────────────────────────────────

  describe('popover toggle', () => {
    beforeEach(() => {
      ;({el, trigger} = createElement(sampleLinks))
    })

    it('sets aria-expanded="true" when menu opens', () => {
      const menu = getMenu(el)

      fireToggleEvent(menu, 'open')

      expect(trigger.getAttribute('aria-expanded')).toBe('true')
    })

    it('sets aria-expanded="false" when menu closes', () => {
      const menu = getMenu(el)

      fireToggleEvent(menu, 'open')
      fireToggleEvent(menu, 'closed')

      expect(trigger.getAttribute('aria-expanded')).toBe('false')
    })

    it('focuses the first menu item when opened', () => {
      const menu = getMenu(el)
      const items = getMenuItems(el)
      const focusSpy = vi.spyOn(items[0], 'focus')

      fireToggleEvent(menu, 'open')

      expect(focusSpy).toHaveBeenCalled()
    })

    it('returns focus to external trigger when closed', () => {
      const menu = getMenu(el)
      const focusSpy = vi.spyOn(trigger, 'focus')

      fireToggleEvent(menu, 'open')
      fireToggleEvent(menu, 'closed')

      expect(focusSpy).toHaveBeenCalled()
    })
  })

  // ──────────────────────────────────────────────
  // Keyboard navigation
  // ──────────────────────────────────────────────

  describe('keyboard navigation', () => {
    beforeEach(() => {
      ;({el, trigger} = createElement(sampleLinks))
    })

    it('ArrowDown moves focus to next item', () => {
      const menu = getMenu(el)
      const items = getMenuItems(el)

      fireToggleEvent(menu, 'open')
      // Simulate active element
      Object.defineProperty(document, 'activeElement', {
        value: items[0],
        configurable: true,
      })

      const focusSpy = vi.spyOn(items[1], 'focus')
      fireKeydown(menu, 'ArrowDown')

      expect(focusSpy).toHaveBeenCalled()
    })

    it('ArrowDown wraps from last to first', () => {
      const menu = getMenu(el)
      const items = getMenuItems(el)

      fireToggleEvent(menu, 'open')
      Object.defineProperty(document, 'activeElement', {
        value: items[2],
        configurable: true,
      })

      const focusSpy = vi.spyOn(items[0], 'focus')
      fireKeydown(menu, 'ArrowDown')

      expect(focusSpy).toHaveBeenCalled()
    })

    it('ArrowUp moves focus to previous item', () => {
      const menu = getMenu(el)
      const items = getMenuItems(el)

      fireToggleEvent(menu, 'open')
      Object.defineProperty(document, 'activeElement', {
        value: items[1],
        configurable: true,
      })

      const focusSpy = vi.spyOn(items[0], 'focus')
      fireKeydown(menu, 'ArrowUp')

      expect(focusSpy).toHaveBeenCalled()
    })

    it('ArrowUp wraps from first to last', () => {
      const menu = getMenu(el)
      const items = getMenuItems(el)

      fireToggleEvent(menu, 'open')
      Object.defineProperty(document, 'activeElement', {
        value: items[0],
        configurable: true,
      })

      const focusSpy = vi.spyOn(items[2], 'focus')
      fireKeydown(menu, 'ArrowUp')

      expect(focusSpy).toHaveBeenCalled()
    })

    it('Home moves focus to first item', () => {
      const menu = getMenu(el)
      const items = getMenuItems(el)

      fireToggleEvent(menu, 'open')
      Object.defineProperty(document, 'activeElement', {
        value: items[2],
        configurable: true,
      })

      const focusSpy = vi.spyOn(items[0], 'focus')
      fireKeydown(menu, 'Home')

      expect(focusSpy).toHaveBeenCalled()
    })

    it('End moves focus to last item', () => {
      const menu = getMenu(el)
      const items = getMenuItems(el)

      fireToggleEvent(menu, 'open')
      Object.defineProperty(document, 'activeElement', {
        value: items[0],
        configurable: true,
      })

      const focusSpy = vi.spyOn(items[2], 'focus')
      fireKeydown(menu, 'End')

      expect(focusSpy).toHaveBeenCalled()
    })

    it('Escape calls hidePopover', () => {
      const menu = getMenu(el)
      menu.hidePopover = vi.fn()

      fireToggleEvent(menu, 'open')
      fireKeydown(menu, 'Escape')

      expect(menu.hidePopover).toHaveBeenCalled()
    })

    it('Tab calls hidePopover', () => {
      const menu = getMenu(el)
      menu.hidePopover = vi.fn()

      fireToggleEvent(menu, 'open')
      fireKeydown(menu, 'Tab')

      expect(menu.hidePopover).toHaveBeenCalled()
    })
  })

  // ──────────────────────────────────────────────
  // Variants
  // ──────────────────────────────────────────────

  describe('variants', () => {
    it('sets data-variant="danger" on danger items', () => {
      ;({el, trigger} = createElement([
        {href: '/delete', label: 'Delete', variant: 'danger'},
      ]))

      const menu = getMenu(el)
      const li = menu.querySelector<HTMLLIElement>('.ellipsis-menu-item')
      expect(li!.dataset.variant).toBe('danger')
    })

    it('sets data-variant="default" when no variant specified', () => {
      ;({el, trigger} = createElement([{href: '/edit', label: 'Edit'}]))

      const menu = getMenu(el)
      const li = menu.querySelector<HTMLLIElement>('.ellipsis-menu-item')
      expect(li!.dataset.variant).toBe('default')
    })
  })

  // ──────────────────────────────────────────────
  // Cleanup
  // ──────────────────────────────────────────────

  describe('cleanup', () => {
    it('removes the menu from the DOM when component is disconnected', () => {
      ;({el, trigger} = createElement(sampleLinks))
      const menu = getMenu(el)
      const menuId = menu.id

      el.remove()

      expect(document.getElementById(menuId)).toBeNull()
    })
  })

  // ──────────────────────────────────────────────
  // Document styles
  // ──────────────────────────────────────────────

  describe('document styles', () => {
    it('injects a style element into the document head', () => {
      ;({el, trigger} = createElement(sampleLinks))
      const style = document.head.querySelector('style[data-ellipsis-menu]')
      expect(style).not.toBeNull()
    })
  })

  // ──────────────────────────────────────────────
  // Unique IDs
  // ──────────────────────────────────────────────

  describe('unique IDs', () => {
    it('each instance has a unique menu id', () => {
      ;({el, trigger} = createElement(sampleLinks))
      const {el: el2, trigger: trigger2} = createElement(sampleLinks)

      const id1 = getMenu(el).id
      const id2 = getMenu(el2).id

      expect(id1).not.toBe(id2)
      expect(id1).toMatch(/^menu-/)
      expect(id2).toMatch(/^menu-/)

      el2.remove()
      trigger2.remove()
    })

    it('external trigger popovertarget matches menu id', () => {
      ;({el, trigger} = createElement(sampleLinks))
      const menu = getMenu(el)

      expect(trigger.getAttribute('popovertarget')).toBe(menu.id)
    })
  })
})
