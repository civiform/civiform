import {ToastController} from '@/toast'

/**
 * Represents session timeout data with timestamps for various timeout events.
 * All timestamps are Unix timestamps in seconds.
 */
interface TimeoutData {
  /** When to show the inactivity warning modal */
  inactivityWarning: number
  /** When to logout due to inactivity */
  inactivityTimeout: number
  /** When to show the total session length warning modal */
  totalWarning: number
  /** When to logout due to total session length */
  totalTimeout: number
  /** Server's current time when cookie was set - used for clock skew calculation */
  currentTime: number
}

/**
 * Types of warning modals that can be shown to the user.
 */
export enum WarningType {
  /** Warning shown when user has been inactive */
  INACTIVITY = 'inactivity',
  /** Warning shown when total session length is approaching limit */
  TOTAL_LENGTH = 'total_length',
}

/**
 * Types of session timeout modals available in the UI.
 * Used for both modal identification and DOM element IDs.
 */
export const enum SessionModalType {
  /** Modal shown for inactivity warnings */
  INACTIVITY = 'session-inactivity-warning',
  /** Modal shown for total session length warnings */
  LENGTH = 'session-length-warning',
}

/**
 * Handles session timeout management including:
 * - Reading timeout data from cookies
 * - Showing warning modals before timeout
 * - Handling session extension requests
 * - Managing automatic logout
 */
export class SessionTimeoutHandler {
  /** Name of cookie storing timeout data */
  private static TIMEOUT_COOKIE_NAME = 'session_timeout_data'
  /** Tracks if inactivity warning is currently shown */
  private static inactivityWarningShown = false
  /** Tracks if total length warning is currently shown */
  private static totalLengthWarningShown = false
  /** Tracks if inactivity warning has been shown at least once */
  private static hasInactivityWarningBeenShown = false
  /** Tracks if total length warning has been shown at least once */
  private static hasTotalLengthWarningBeenShown = false
  /** Tracks if handler has been initialized */
  private static isInitialized = false

  static init() {
    if (this.isInitialized) {
      return
    }

    this.setupModalEventHandlers()
    this.pollSession()
    this.isInitialized = true
  }

  private static monitorSession(data: TimeoutData | null, now: number) {
    if (!data) {
      console.warn('No session timeout data available')
      return
    }

    // 1. If there is an inactivityTimeout or totalTimeout that has passed, just logout
    if (data.inactivityTimeout <= now || data.totalTimeout <= now) {
      this.logout()
      return
    }

    // If a warning is currently being shown, don't show another one
    if (this.inactivityWarningShown || this.totalLengthWarningShown) {
      return
    }

    // 2 & 3. Show warnings if they haven't been shown before and their time has passed
    if (!this.hasInactivityWarningBeenShown && data.inactivityWarning <= now) {
      this.showWarning(WarningType.INACTIVITY)
      this.inactivityWarningShown = true
      this.hasInactivityWarningBeenShown = true
      return
    }

    if (!this.hasTotalLengthWarningBeenShown && data.totalWarning <= now) {
      this.showWarning(WarningType.TOTAL_LENGTH)
      this.totalLengthWarningShown = true
      this.hasTotalLengthWarningBeenShown = true
      return
    }
  }

  private static pollSession() {
    const data = this.getTimeoutData()

    const now = Math.floor(Date.now() / 1000)
    try {
      this.monitorSession(data, now)
    } catch (e) {
      console.error('Error monitoring session:', e)
      // If an error is thrown, do not continue polling
      return
    }

    window.setTimeout(() => {
      this.pollSession()
    }, 30000) // Check every 30 seconds
  }

  /**
   * Set up event handlers for the server-rendered modals and process the extend session form submission.
   */
  private static setupModalEventHandlers() {
    // HTMX handler remains at document level for form submissions
    document.addEventListener('htmx:afterRequest', (event: Event) => {
      const customEvent = event as CustomEvent
      const detail = customEvent.detail as {
        xhr: XMLHttpRequest
        elt: HTMLElement
      }
      if (detail.elt.id !== 'extend-session-form') return

      const inactivityModal = document.getElementById(
        `${SessionModalType.INACTIVITY}-modal`,
      )
      inactivityModal?.classList.add('is-hidden')
      this.inactivityWarningShown = false

      // Processes /extend-session form submissions
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
        this.pollSession()
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

    // Set up handlers for inactivity warning modal
    const inactivityModal = document.getElementById(
      `${SessionModalType.INACTIVITY}-modal`,
    )
    if (inactivityModal) {
      // Handle extend session button
      const extendButton = inactivityModal.querySelector('[data-modal-primary]')
      extendButton?.addEventListener('click', () => {
        const form = document.getElementById(
          'extend-session-form',
        ) as HTMLFormElement
        form?.requestSubmit()
      })

      // Handle dismiss/close buttons
      const closeButtons = inactivityModal.querySelectorAll(
        '[data-modal-secondary], [data-close-modal]',
      )
      closeButtons.forEach((button) => {
        button.addEventListener('click', () => {
          inactivityModal.classList.add('is-hidden')
          this.inactivityWarningShown = false
          this.pollSession()
        })
      })
    }

    // Set up handlers for session length warning modal
    const lengthModal = document.getElementById(
      `${SessionModalType.LENGTH}-modal`,
    )
    if (lengthModal) {
      // Handle logout button
      const logoutButton = lengthModal.querySelector('[data-modal-primary]')
      logoutButton?.addEventListener('click', () => {
        this.logout()
      })

      // Handle dismiss/close buttons
      const closeButtons = lengthModal.querySelectorAll(
        '[data-modal-secondary], [data-close-modal]',
      )
      closeButtons.forEach((button) => {
        button.addEventListener('click', () => {
          lengthModal.classList.add('is-hidden')
          this.totalLengthWarningShown = false
          this.pollSession()
        })
      })
    }
  }

  /**
   * Reads the session timeout data from the cookie.
   */
  private static getTimeoutData(): TimeoutData | null {
    const cookieValue = this.getCookie(this.TIMEOUT_COOKIE_NAME)
    if (!cookieValue) return null

    try {
      // Decode Base64 and parse JSON
      const jsonString = atob(decodeURIComponent(cookieValue))
      const data: unknown = JSON.parse(jsonString)
      if (!this.isTimeoutData(data)) {
        console.error('Invalid timeout data format')
        return null
      }
      return {
        inactivityWarning: data.inactivityWarning,
        inactivityTimeout: data.inactivityTimeout,
        totalWarning: data.totalWarning,
        totalTimeout: data.totalTimeout,
        currentTime: data.currentTime,
      }
    } catch (e) {
      console.error('Failed to parse session timeout data:', e)
      return null
    }
  }

  /**
   * Type predicate function to check if an unknown value is a TimeoutData object.
   *
   * @param data The value to check.
   * @returns `true` if the value is a TimeoutData object, `false` otherwise.
   */
  private static isTimeoutData(data: unknown): data is TimeoutData {
    // Check if the data is falsy or not an object. If so, it cannot be TimeoutData.
    if (!data || typeof data !== 'object') {
      return false
    }

    // Treat the data as a Record to allow accessing properties.
    const d = data as Record<string, unknown>

    // Check if all required properties exist and are of the correct type (number).
    return (
      typeof d.inactivityWarning === 'number' &&
      typeof d.inactivityTimeout === 'number' &&
      typeof d.totalWarning === 'number' &&
      typeof d.totalTimeout === 'number' &&
      typeof d.currentTime === 'number'
    )
  }

  /**
   * Retrieves the value of a cookie by its name.
   *
   * This function parses the `document.cookie` string to find the value
   * associated with the given cookie name.
   *
   * @param name The name of the cookie to retrieve.
   * @returns The string value of the cookie, or null if the cookie is not found.
   */
  private static getCookie(name: string): string | null {
    // Prefix the document cookie string with a semicolon and a space to handle case when our cookie is the first.
    const value = `; ${document.cookie}`

    // Split the cookie string into an array of parts, using the cookie name as the delimiter.
    // This assumes the cookie name is followed by an equals sign (=).
    const parts = value.split(`; ${name}=`)

    // Check if the cookie was found. If parts.length is 2, it means the cookie name
    // was found, and the array contains a part before and a part after the cookie name.
    if (parts.length === 2) {
      // Extract the cookie value by taking the last element of the 'parts' array (the part after the cookie name),
      // then splitting it by semicolons to separate the value from any subsequent cookie attributes
      // (e.g., path, domain, etc.). Finally, take the first element (the actual value) and return it.
      // If any of the split operations fail to return a value, return null.
      return parts.pop()?.split(';').shift() || null
    }

    return null
  }

  /**
   * Shows a warning modal of the specified type.
   *
   * @param type Type of warning to show (inactivity or total length)
   */
  private static showWarning(type: WarningType) {
    const modalId =
      type === WarningType.INACTIVITY
        ? `${SessionModalType.INACTIVITY}-modal`
        : `${SessionModalType.LENGTH}-modal`
    const modal = document.getElementById(modalId)
    if (!modal) {
      throw new Error(`Modal with ID ${modalId} not found`)
    }
    modal.classList.remove('is-hidden')
  }

  /**
   * Initiates logout.
   */
  private static logout() {
    window.location.href = '/logout'
  }
}
