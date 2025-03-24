import {ToastController} from './toast'
import {hideUswdsModal} from './modal'

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
  /** Current active timeout timer */
  private static timer: number | null = null
  /** Tracks if handler has been initialized */
  private static isInitialized = false

  /** Stores the next timeout action and time for testing */
  private static nextTimeoutAction: (() => void) | null = null
  private static nextTimeoutTime: number | null = null

  static init() {
    if (this.isInitialized) {
      return
    }

    void this.checkAndSetTimer()
    this.setupModalEventHandlers()
    // Add listener for timechange event to support testing with mocked time
    window.addEventListener('timechange', () => {
      // First check if we need to immediately logout due to timeout
      const data = this.getTimeoutData()
      if (data) {
        const now = Math.floor(Date.now() / 1000)
        // Check for timeout conditions first
        if (data.inactivityTimeout <= now || data.totalTimeout <= now) {
          this.handleTimeout()
          return
        }
      }

      // If we have stored the next timeout action and time, check if it should fire
      if (this.nextTimeoutAction && this.nextTimeoutTime) {
        const now = Math.floor(Date.now() / 1000)
        if (now >= this.nextTimeoutTime) {
          const action = this.nextTimeoutAction
          this.nextTimeoutAction = null
          this.nextTimeoutTime = null
          action()
        }
      }
      // Always recheck timeouts after time change
      void this.checkAndSetTimer()
    })
    this.isInitialized = true
  }

  /**
   * Main timer management method that:
   * 1. Checks for immediate timeout conditions and logs out if needed
   * 2. Shows inactivity warning once if its time has passed and not shown before
   * 3. Shows total length warning once if its time has passed and not shown before
   * 4. Sets timer for next future event (warning or timeout)
   *
   * Warning dialogs are shown only once per session and only one dialog
   * can be visible at a time. If a warning was previously shown, it won't
   * be shown again even if its time passes again.
   */
  private static checkAndSetTimer() {
    const data = this.getTimeoutData()
    if (!data) return

    // Clear existing timer
    if (this.timer) {
      window.clearTimeout(this.timer)
      this.timer = null
    }

    const now = Math.floor(Date.now() / 1000)

    // 1. If there is an inactivityTimeout or totalTimeout that has passed, just logout
    if (data.inactivityTimeout <= now || data.totalTimeout <= now) {
      this.handleTimeout()
      return
    }

    // If a warning is already being shown, don't show another one
    if (this.inactivityWarningShown || this.totalLengthWarningShown) {
      return
    }

    // 2 & 3. Show warnings if they haven't been shown before and their time has passed
    if (!this.hasInactivityWarningBeenShown && data.inactivityWarning <= now) {
      this.showWarning(WarningType.INACTIVITY)
      this.hasInactivityWarningBeenShown = true
      return
    }

    if (!this.hasTotalLengthWarningBeenShown && data.totalWarning <= now) {
      this.showWarning(WarningType.TOTAL_LENGTH)
      this.hasTotalLengthWarningBeenShown = true
      return
    }

    // Set timers for future events
    const timeouts = []

    // Only add inactivity warning if it hasn't been shown yet
    if (!this.hasInactivityWarningBeenShown && data.inactivityWarning > now) {
      timeouts.push({
        time: data.inactivityWarning,
        action: () => {
          this.showWarning(WarningType.INACTIVITY)
          this.hasInactivityWarningBeenShown = true
        },
        type: WarningType.INACTIVITY,
      })
    }

    if (!this.hasTotalLengthWarningBeenShown && data.totalWarning > now) {
      timeouts.push({
        time: data.totalWarning,
        action: () => {
          this.showWarning(WarningType.TOTAL_LENGTH)
          this.hasTotalLengthWarningBeenShown = true
        },
        type: WarningType.TOTAL_LENGTH,
      })
    }

    // Always add timeout events
    timeouts.push({
      time: data.inactivityTimeout,
      action: () => this.handleTimeout(),
      type: 'timeout',
    })

    timeouts.push({
      time: data.totalTimeout,
      action: () => this.handleTimeout(),
      type: 'timeout',
    })

    // Sort by earliest time
    timeouts.sort((a, b) => a.time - b.time)

    // We will always have at least one event in the future
    const nextTimeout = timeouts[0]
    const delay = (nextTimeout.time - now) * 1000

    // Store the next timeout action and time for handling timechange event.
    this.nextTimeoutAction = nextTimeout.action
    this.nextTimeoutTime = nextTimeout.time

    this.timer = window.setTimeout(() => {
      this.nextTimeoutAction = null
      this.nextTimeoutTime = null
      nextTimeout.action()

      // Check for next timeout after handling this one
      this.checkAndSetTimer()
    }, delay)
  }

  /**
   * Set up event handlers for the server-rendered modals and process the extend session form submission.
   */
  private static setupModalEventHandlers() {
    // HTMX handler remains at document level for form submissions
    document.addEventListener('htmx:afterRequest', (event: Event) => {
      // ...existing htmx handler code...
      const customEvent = event as CustomEvent
      const detail = customEvent.detail as {
        xhr: XMLHttpRequest
        elt: HTMLElement
      }
      if (detail.elt.id !== 'extend-session-form') return

      hideUswdsModal(SessionModalType.INACTIVITY)
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
          hideUswdsModal(SessionModalType.INACTIVITY)
          this.inactivityWarningShown = false
          this.checkAndSetTimer()
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
          hideUswdsModal(SessionModalType.LENGTH)
          this.totalLengthWarningShown = false
          this.checkAndSetTimer()
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
   * Shows a warning modal of specified type.
   * Only shows the modal if:
   * - No other warning is currently shown
   * - The modal element exists in the DOM
   *
   * Updates visibility tracking flags when showing a modal.
   *
   * @param type Type of warning to show (inactivity or total length)
   */
  private static showWarning(type: WarningType) {
    // Check if any warning is already shown to prevent showing multiple dialogs
    if (this.inactivityWarningShown || this.totalLengthWarningShown) return

    // Get the appropriate modal element
    const modalId =
      type === WarningType.INACTIVITY
        ? `${SessionModalType.INACTIVITY}-modal`
        : `${SessionModalType.LENGTH}-modal`
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
  }

  /**
   * Handles timeout conditions by calling logout.
   */
  private static handleTimeout() {
    this.logout()
  }

  /**
   * Initiates logout.
   */
  private static logout() {
    window.location.href = '/logout'
  }
}
