export class Ellipsis_menu extends HTMLElement {
  private _trigger!: HTMLButtonElement
  private _menu!: HTMLUListElement

  // Shared document styles — injected once
  private static _documentStylesInjected = false

  private static injectDocumentStyles(): void {
    if (Ellipsis_menu._documentStylesInjected) return
    Ellipsis_menu._documentStylesInjected = true

    const style = document.createElement('style')
    style.setAttribute('data-ellipsis-menu', '')
    style.textContent = `
      .ellipsis-menu {
        position: fixed;
        inset: auto;
        position-area: block-end span-inline-end;
        position-try-fallbacks:
          block-end span-inline-start,
          block-start span-inline-end,
          block-start span-inline-start;
        margin: 6px 0;
        padding: 4px;
        border: 1px solid #e8e5e0;
        border-radius: 12px;
        background: #fff;
        box-shadow:
          0 4px 16px rgba(0,0,0,0.08),
          0 1px 3px rgba(0,0,0,0.04);
        min-width: 180px;
        list-style: none;
        z-index: 1000;

        opacity: 0;
        transform: scale(0.95) translateY(-4px);
        transition:
          opacity 0.15s ease,
          transform 0.15s ease,
          overlay 0.15s ease allow-discrete,
          display 0.15s ease allow-discrete;
      }

      .ellipsis-menu:popover-open {
        opacity: 1;
        transform: scale(1) translateY(0);
      }

      @starting-style {
        .ellipsis-menu:popover-open {
          opacity: 0;
          transform: scale(0.95) translateY(-4px);
        }
      }

      .ellipsis-menu-item {
        display: block;
      }

      .ellipsis-menu-item a,
      .ellipsis-menu-item button {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        width: 100%;
        padding: 8px 12px;
        border: none;
        border-radius: 8px;
        background: none;
        font-family: inherit;
        font-size: 0.85rem;
        font-weight: 500;
        color: #1a1715;
        text-decoration: none;
        text-align: start;
        cursor: pointer;
        transition: background 0.12s ease;
        white-space: nowrap;
      }

      .ellipsis-menu-item a:hover,
      .ellipsis-menu-item a:focus-visible,
      .ellipsis-menu-item button:hover,
      .ellipsis-menu-item button:focus-visible {
        background: #f5f3ef;
      }

      .ellipsis-menu-item a:focus-visible,
      .ellipsis-menu-item button:focus-visible {
        outline: 2px solid #6b6560;
        outline-offset: -2px;
      }

      .ellipsis-menu-item[data-variant="danger"] a,
      .ellipsis-menu-item[data-variant="danger"] button {
        color: #c0392b;
      }

      .ellipsis-menu-item[data-variant="danger"] a:hover,
      .ellipsis-menu-item[data-variant="danger"] a:focus-visible,
      .ellipsis-menu-item[data-variant="danger"] button:hover,
      .ellipsis-menu-item[data-variant="danger"] button:focus-visible {
        background: #fdf2f0;
      }
    `
    document.head.appendChild(style)
  }

  connectedCallback(): void {
    const triggerId = this.getAttribute('trigger')
    if (!triggerId) {
      throw new Error(
        'wc-ellipsis-menu requires a "trigger" attribute pointing to a button id',
      )
    }

    const trigger = document.getElementById(
      triggerId,
    ) as HTMLButtonElement | null
    if (!trigger) {
      throw new Error(
        `wc-ellipsis-menu: no element found with id "${triggerId}"`,
      )
    }
    this._trigger = trigger

    const id = `menu-${Math.random().toString(36).slice(2, 10)}`

    // Inject shared document styles once
    Ellipsis_menu.injectDocumentStyles()

    // Apply anchor-name to the external trigger
    // @ts-ignore — anchorName exists but TS types lag behind
    this._trigger.style.anchorName = `--${id}`

    // Wire up popover targeting
    this._trigger.setAttribute('popovertarget', id)

    // Set accessibility attributes on the external trigger if not already present
    if (!this._trigger.hasAttribute('aria-haspopup')) {
      this._trigger.setAttribute('aria-haspopup', 'true')
    }
    if (!this._trigger.hasAttribute('aria-expanded')) {
      this._trigger.setAttribute('aria-expanded', 'false')
    }

    const ariaLabel = this.getAttribute('aria-label')

    // Build the menu in the light DOM so popovertarget can find it
    const menu = document.createElement('ul')
    menu.id = id
    menu.className = 'ellipsis-menu'
    menu.setAttribute('popover', '')
    menu.setAttribute('role', 'menu')

    if (ariaLabel) {
      menu.setAttribute('aria-label', ariaLabel)
    } else {
      menu.setAttribute('aria-label', 'Options')
    }

    // @ts-ignore — positionAnchor exists but TS types lag behind
    menu.style.positionAnchor = `--${id}`

    // Move authored <li> children into the popover menu
    // Note: <li> must be wrapped in a <menu> or <ul> in the markup,
    // otherwise the HTML parser hoists them out of the custom element.
    const items = Array.from(this.querySelectorAll('li'))
    for (const li of items) {
      li.classList.add('ellipsis-menu-item')
      li.setAttribute('role', 'none')

      // Set menuitem role + tabindex on the focusable child (a or button)
      const focusable = li.querySelector('a, button')
      if (focusable) {
        focusable.setAttribute('role', 'menuitem')
        focusable.setAttribute('tabindex', '-1')
      }

      menu.appendChild(li)
    }

    document.body.appendChild(menu)
    this._menu = menu

    // Sync aria-expanded with popover state
    this._menu.addEventListener('toggle', (e: Event) => {
      const open = (e as ToggleEvent).newState === 'open'
      this._trigger.setAttribute('aria-expanded', String(open))

      if (open) {
        const firstItem = this._menu.querySelector<HTMLElement>('a, button')
        firstItem?.focus()
      } else {
        this._trigger.focus()
      }
    })

    // Keyboard navigation inside the menu
    this._menu.addEventListener('keydown', (e: KeyboardEvent) => {
      const items = Array.from(
        this._menu.querySelectorAll<HTMLElement>('a, button'),
      )
      const current = document.activeElement as HTMLElement | null
      const idx = current ? items.indexOf(current) : -1

      switch (e.key) {
        case 'ArrowDown': {
          e.preventDefault()
          const next = items[(idx + 1) % items.length]
          next.focus()
          break
        }
        case 'ArrowUp': {
          e.preventDefault()
          const prev = items[(idx - 1 + items.length) % items.length]
          prev.focus()
          break
        }
        case 'Home': {
          e.preventDefault()
          items[0]?.focus()
          break
        }
        case 'End': {
          e.preventDefault()
          items[items.length - 1]?.focus()
          break
        }
        case 'Escape': {
          e.preventDefault()
          this._menu.hidePopover()
          break
        }
        case 'Tab': {
          this._menu.hidePopover()
          break
        }
      }
    })
  }

  disconnectedCallback(): void {
    this._menu?.remove()
  }
}

customElements.define('wc-ellipsis-menu', Ellipsis_menu)
