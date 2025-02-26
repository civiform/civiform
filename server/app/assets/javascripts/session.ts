import {ToastController} from './toast'
import {assertNotNull} from './util'

interface TimeoutData {
  inactivityWarning: number
  inactivityTimeout: number
  totalWarning: number
  totalTimeout: number
  currentTime: number
}

export enum WarningType {
  INACTIVITY = 'inactivity',
  TOTAL_LENGTH = 'total_length',
}

export class SessionTimeoutHandler {
  private static COOKIE_NAME = 'session_timeout_data'
  private static CHECK_INTERVAL = 2000 // 2 seconds
  private static warningShown = false
  private static timer: number | null = null

  static init() {
    void this.checkAndSetTimer()
  }

  private static getTimeoutData(): TimeoutData | null {
    const cookieValue = this.getCookie(this.COOKIE_NAME)
    if (!cookieValue) return null

    try {
      // Decode Base64 and parse JSON
      const jsonString = atob(decodeURIComponent(cookieValue))
      const data: unknown = JSON.parse(jsonString)
      if (!this.isTimeoutData(data)) {
        console.error('Invalid timeout data format')
        return null
      }
      // Calculate clock skew between client and server
      const clientNow = Math.floor(Date.now() / 1000)
      const clockSkew = clientNow - data.currentTime
      return {
        inactivityWarning: data.inactivityWarning + clockSkew,
        inactivityTimeout: data.inactivityTimeout + clockSkew,
        totalWarning: data.totalWarning + clockSkew,
        totalTimeout: data.totalTimeout + clockSkew,
        currentTime: data.currentTime,
      }
    } catch (e) {
      console.error('Failed to parse session timeout data:', e)
      return null
    }
  }

  private static isTimeoutData(data: unknown): data is TimeoutData {
    if (!data || typeof data !== 'object') return false

    const d = data as Record<string, unknown>

    return (
      typeof d.inactivityWarning === 'number' &&
      typeof d.inactivityTimeout === 'number' &&
      typeof d.totalWarning === 'number' &&
      typeof d.totalTimeout === 'number' &&
      typeof d.currentTime === 'number'
    )
  }

  private static getCookie(name: string): string | null {
    const value = `; ${document.cookie}`
    const parts = value.split(`; ${name}=`)
    if (parts.length === 2) {
      return parts.pop()?.split(';').shift() || null
    }
    return null
  }

  private static checkAndSetTimer() {
    const data = this.getTimeoutData()
    if (!data) return

    // Clear existing timer
    if (this.timer) {
      window.clearTimeout(this.timer)
      this.timer = null
    }

    const now = Math.floor(Date.now() / 1000)

    const timeouts = [
      {time: data.inactivityWarning, type: WarningType.INACTIVITY},
      {time: data.totalWarning, type: WarningType.TOTAL_LENGTH},
      {time: data.inactivityTimeout, type: null},
      {time: data.totalTimeout, type: null},
    ].filter((t) => t.time > now)

    // Sort by earliest time
    timeouts.sort((a, b) => a.time - b.time)

    if (timeouts.length === 0) {
      this.handleTimeout()
      return
    }

    const nextTimeout = timeouts[0]
    const delay = (nextTimeout.time - now) * 1000

    this.timer = window.setTimeout(() => {
      if (nextTimeout.type) {
        this.showWarning(nextTimeout.type)
      } else {
        void this.handleTimeout()
      }
    }, delay)
  }

  private static showWarning(type: WarningType) {
    if (this.warningShown) return
    this.warningShown = true

    const modalId = 'session-timeout-modal'
    const headingId = 'session-timeout-heading'
    const descriptionId = 'session-timeout-description'

    // Create modal overlay
    const modalOverlay = document.createElement('div')
    modalOverlay.className = 'usa-modal-wrapper'
    modalOverlay.style.cssText = `
        position: fixed;
        left: 0;
        top: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 99999;
    `

    // Create modal container following USWDS pattern
    const modalContainer = document.createElement('div')
    modalContainer.className = 'usa-modal'
    modalContainer.id = modalId
    modalContainer.setAttribute('aria-labelledby', headingId)
    modalContainer.setAttribute('aria-describedby', descriptionId)
    modalContainer.style.cssText = `
        position: relative;
        margin: auto;
        transform: translateY(25%);
        background: white;
        max-width: 500px;
        width: 90%;
    `

    // Create modal content wrapper
    const modalContent = document.createElement('div')
    modalContent.className = 'usa-modal__content'

    // Create main content area
    const mainContent = document.createElement('div')
    mainContent.className = 'usa-modal__main mx-4'

    // Create heading
    const heading = document.createElement('h2')
    heading.className = 'usa-modal__heading'
    heading.id = headingId
    heading.textContent =
      type === WarningType.INACTIVITY
        ? 'Session Inactivity Warning'
        : 'Session Length Warning'

    // Create description
    const description = document.createElement('div')
    description.className = 'usa-prose my-6'
    description.id = descriptionId

    const message = document.createElement('p')
    message.textContent =
      type === WarningType.INACTIVITY
        ? 'Your session will expire soon due to inactivity. Would you like to extend your session?'
        : 'Your session will expire soon due to length. Please save your work and re-login if needed.'
    description.appendChild(message)

    // Create footer
    const footer = document.createElement('div')
    footer.className = 'usa-modal__footer'

    const buttonGroup = document.createElement('ul')
    buttonGroup.className = 'usa-button-group'

    // Create primary button (Extend Session or Logout)
    // Create primary button (Extend Session or Logout)
    const primaryButtonItem = document.createElement('li')
    primaryButtonItem.className = 'usa-button-group__item'

    if (type === WarningType.INACTIVITY) {
      const form = document.createElement('form')
      form.id = 'extend-session-form'
      form.setAttribute('hx-post', '/extend-session')
      form.setAttribute('hx-target', '#extend-session-form')
      form.setAttribute('hx-swap', 'none')
      // Clone the existing CSRF token input
      const csrfInput = assertNotNull(
        document.querySelector('input[name=csrfToken]'),
        'csrf token',
      ).cloneNode(true) as HTMLInputElement
      form.appendChild(csrfInput)
      const submitButton = document.createElement('button')
      submitButton.type = 'submit'
      submitButton.className = 'usa-button'
      submitButton.textContent = 'Extend Session'
      form.appendChild(submitButton)

      form.addEventListener('htmx:afterRequest', (event: Event) => {
        const customEvent = event as CustomEvent
        const detail = customEvent.detail as {xhr: XMLHttpRequest}
        const xhr = detail.xhr
        modalOverlay.remove()
        this.warningShown = false
        if (xhr.status === 200) {
          ToastController.showToastMessage({
            id: 'session-extended-toast',
            content: 'Session successfully extended',
            type: 'success',
            duration: 3000,
            canDismiss: true,
            canIgnore: false,
            condOnStorageKey: null,
          })
          this.checkAndSetTimer()
        } else {
          ToastController.showToastMessage({
            id: 'session-extend-error-toast',
            content: 'Failed to extend session',
            type: 'error',
            duration: 3000,
            canDismiss: true,
            canIgnore: false,
            condOnStorageKey: null,
          })
          console.error('Failed to extend session:', xhr.statusText)
        }
      })

      primaryButtonItem.appendChild(form)
    } else {
      const logoutButton = document.createElement('button')
      logoutButton.className = 'usa-button'
      logoutButton.textContent = 'Logout'
      logoutButton.onclick = this.logout.bind(this)
      primaryButtonItem.appendChild(logoutButton)
    }

    // Create secondary button (Cancel/Close)
    const secondaryButtonItem = document.createElement('li')
    secondaryButtonItem.className = 'usa-button-group__item'

    const secondaryButton = document.createElement('button')
    secondaryButton.className =
      'usa-button usa-button--unstyled padding-105 text-center'
    secondaryButton.setAttribute('type', 'button')
    secondaryButton.setAttribute('data-close-modal', '')
    secondaryButton.textContent = 'Cancel'
    secondaryButton.onclick = () => {
      modalOverlay.remove()
      this.warningShown = false
    }
    secondaryButtonItem.appendChild(secondaryButton)

    // Assemble the modal
    buttonGroup.appendChild(primaryButtonItem)
    buttonGroup.appendChild(secondaryButtonItem)
    footer.appendChild(buttonGroup)

    mainContent.appendChild(heading)
    mainContent.appendChild(description)
    mainContent.appendChild(footer)

    modalContent.appendChild(mainContent)
    modalContainer.appendChild(modalContent)
    // Add click handler to overlay for closing
    modalOverlay.addEventListener('click', (e) => {
      if (e.target === modalOverlay) {
        modalOverlay.remove()
        this.warningShown = false
        this.checkAndSetTimer()
      }
    })
    modalOverlay.appendChild(modalContainer)
    document.body.appendChild(modalOverlay)
  }

  private static handleTimeout() {
    this.logout()
  }

  private static logout() {
    window.location.href = '/logout'
  }
}
