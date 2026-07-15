/**
 * Manages the layout-level alert container (`#alertContainer`), the common
 * region for page-level errors and notifications. Server-rendered alerts and
 * htmx out-of-band swaps land in the same container; this module covers
 * alerts that originate on the client (e.g. the form validation error
 * summary).
 *
 * The container is an `aria-live="polite"` region, so inserted content is
 * announced without needing `role="alert"` on individual alerts. New content
 * replaces whatever is currently in the container; alerts do not stack.
 */

export type AlertContainerAlertType = 'error' | 'info' | 'success' | 'warning'

export interface ErrorSummaryItem {
  /** Message shown for this error in the summary list. */
  message: string
  /** Id of the form control the message refers to. When set, the summary entry links to and focuses the control. */
  controlId?: string
}

const CONTAINER_ID = 'alertContainer'

export class AlertContainer {
  /**
   * Observes the container so alerts inserted after page load — by htmx
   * out-of-band swaps or by other scripts — are scrolled into view. Without
   * this, an alert triggered from far down a long page appears off-screen.
   */
  static init() {
    const container = getContainer()

    if (!container) {
      return
    }

    const observer = new MutationObserver(() => {
      const alert = container.querySelector<HTMLElement>('.usa-alert')
      if (alert) {
        alert.scrollIntoView?.({behavior: 'smooth', block: 'nearest'})
      }
    })

    observer.observe(container, {childList: true})
  }

  /**
   * Replaces the container content with a single slim alert.
   *
   * @returns false when the page has no alert container.
   */
  static show(type: AlertContainerAlertType, text: string): boolean {
    const container = getContainer()

    if (!container) {
      return false
    }

    container.replaceChildren(buildAlert(type, text))
    return true
  }

  /**
   * Replaces the container content with an error summary listing each failing
   * control, then moves focus to the summary so it is announced and visible.
   *
   * @returns false when the page has no alert container.
   */
  static showErrorSummary(heading: string, items: ErrorSummaryItem[]): boolean {
    const container = getContainer()

    if (!container) {
      return false
    }

    const summary = buildErrorSummary(heading, items)
    container.replaceChildren(summary)
    summary.focus()
    return true
  }

  /** Empties the container. */
  static clear() {
    getContainer()?.replaceChildren()
  }
}

function getContainer(): HTMLElement | null {
  return document.getElementById(CONTAINER_ID)
}

function buildAlert(
  type: AlertContainerAlertType,
  text: string,
): HTMLDivElement {
  const alert = document.createElement('div')
  alert.className = `usa-alert usa-alert--slim usa-alert--${type}`

  const body = document.createElement('div')
  body.className = 'usa-alert__body'

  const alertText = document.createElement('p')
  alertText.className = 'usa-alert__text'
  alertText.textContent = text

  body.appendChild(alertText)
  alert.appendChild(body)
  return alert
}

function buildErrorSummary(
  heading: string,
  items: ErrorSummaryItem[],
): HTMLDivElement {
  const alert = document.createElement('div')
  alert.className = 'usa-alert usa-alert--error'
  // Focusable so focus can move to the summary on failed submit without
  // adding it to the tab order.
  alert.tabIndex = -1

  const body = document.createElement('div')
  body.className = 'usa-alert__body'

  const headingEl = document.createElement('h2')
  headingEl.className = 'usa-alert__heading'
  headingEl.textContent = heading
  body.appendChild(headingEl)

  const list = document.createElement('ul')
  list.className = 'usa-list margin-top-0'

  items.forEach((item) => {
    const entry = document.createElement('li')

    if (item.controlId) {
      const controlId = item.controlId
      const link = document.createElement('a')
      link.href = `#${controlId}`
      link.textContent = item.message
      link.addEventListener('click', (event: MouseEvent) => {
        event.preventDefault()
        document.getElementById(controlId)?.focus()
      })
      entry.appendChild(link)
    } else {
      entry.textContent = item.message
    }

    list.appendChild(entry)
  })

  body.appendChild(list)
  alert.appendChild(body)
  return alert
}
