import {ToastController} from './toast'

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
  private static inactivityWarningShown = false
  private static totalLengthWarningShown = false
  private static timer: number | null = null

  static init() {
    void this.checkAndSetTimer()
    this.setupModalEventHandlers()
  }

  /**
   * Set up event handlers for the server-rendered modals.
   */
  private static setupModalEventHandlers() {
    document.addEventListener('htmx:afterRequest', (event: Event) => {
      const customEvent = event as CustomEvent
      const detail = customEvent.detail as {
        xhr: XMLHttpRequest
        elt: HTMLElement
      }
      if (detail.elt.id !== 'extend-session-form') return

      this.hideModal('session-inactivity-warning')
      this.inactivityWarningShown = false

      if (detail.xhr.status === 200) {
        const successText =
          document.getElementById('session-extended-success-text')
            ?.textContent || 'Session successfully extended'
        ToastController.showToastMessage({
          id: 'session-extended-toast',
          content: successText,
          type: 'success',
          duration: 3000,
          canDismiss: true,
          canIgnore: false,
          condOnStorageKey: null,
        })
        this.checkAndSetTimer()
      } else {
        const errorText =
          document.getElementById('session-extended-error-text')?.textContent ||
          'Failed to extend session'
        ToastController.showToastMessage({
          id: 'session-extend-error-toast',
          content: errorText,
          type: 'error',
          duration: 3000,
          canDismiss: true,
          canIgnore: false,
          condOnStorageKey: null,
        })
      }
    })

    // Handle modal actions using event delegation
    document.addEventListener('click', (event) => {
      const target = event.target as HTMLElement
      if (!target) return

      const modalType = target.getAttribute('data-modal-type')
      if (!modalType) return

      if (target.hasAttribute('data-modal-primary')) {
        if (modalType === 'session-inactivity-warning') {
          // Handle extend session
          const form = document.getElementById(
            'extend-session-form',
          ) as HTMLFormElement
          form?.requestSubmit()
        } else if (modalType === 'session-length-warning') {
          this.logout()
        }
      } else if (
        target.hasAttribute('data-modal-secondary') ||
        target.hasAttribute('data-close-modal')
      ) {
        this.hideModal(modalType)
        if (modalType === 'session-inactivity-warning') {
          this.inactivityWarningShown = false
        } else if (modalType === 'session-length-warning') {
          this.totalLengthWarningShown = false
        }
        this.checkAndSetTimer()
      }
    })
  }

  private static hideModal(modalType: string) {
    const modal = document.getElementById(`${modalType}-modal`)
    if (modal) {
      modal.classList.add('is-hidden')
    }
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
    // Check if the warning is already shown to prevent duplicate dialogs
    if (type === WarningType.INACTIVITY && this.inactivityWarningShown) return
    if (type === WarningType.TOTAL_LENGTH && this.totalLengthWarningShown)
      return
    // Get the appropriate modal element
    const modalId =
      type === WarningType.INACTIVITY
        ? 'session-inactivity-warning-modal'
        : 'session-length-warning-modal'
    const modal = document.getElementById(modalId)
    if (!modal) {
      console.error(`Modal with ID ${modalId} not found`)
      return
    }
    // Show the modal by removing the hidden class
    modal.classList.remove('is-hidden')
    // Set the flag to indicate that the warning is shown
    if (type === WarningType.INACTIVITY) {
      this.inactivityWarningShown = true
    } else {
      this.totalLengthWarningShown = true
    }
    // Continue checking for other timeouts
    this.checkAndSetTimer()
  }

  private static handleTimeout() {
    this.logout()
  }

  private static logout() {
    window.location.href = '/logout'
  }
}
