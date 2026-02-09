import {ToastController} from '@/toast'

/**
 * There are two types of session timeouts, each with its own warning modal.
 * Timeout durations are configured in application.conf and read from a cookie set by the server.
 *
 * Inactivity timeout (can be extended)
 *   - Appears after a period of no server activity
 *   - User can click "Extend Session", which calls the server to reset the session's last activity timestamp
 *   - If extended, modal will re-appear when the next inactivity timeout approaches
 *   - User can dismiss the modal, in which case they will be logged out when the inactivity timestamp passes
 *
 * Total session length timeout (cannot be extended)
 *   - Appears when the session approaches the maximum allowed duration
 *   - User can click "Log Out" or dismiss the modal
 *   - If dismissed, user will be logged out when the total timeout passes
 *   - Only shown once per session
 *
 * The handler polls every 30 seconds to check timeout thresholds.
 */

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
  /** sessionStorage key for tracking shown inactivity warning timestamp */
  private static INACTIVITY_WARNING_SHOWN_KEY =
    'session_inactivity_warning_shown'
  /** sessionStorage key for tracking shown total warning timestamp */
  private static TOTAL_WARNING_SHOWN_KEY = 'session_total_warning_shown'
  /** Tracks if inactivity warning is currently visible */
  private static inactivityWarningVisible = false
  /** Tracks if total length warning is currently visible */
  private static totalLengthWarningVisible = false
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

  private static monitorSession(data: TimeoutData, now: number) {
    // 1. If there is an inactivityTimeout or totalTimeout that has passed, just logout
    if (data.inactivityTimeout <= now || data.totalTimeout <= now) {
      this.logout()
      return
    }

    // If a warning is currently visible, don't show another one
    if (this.inactivityWarningVisible || this.totalLengthWarningVisible) {
      return
    }

    // Show inactivity warning if threshold passed, not already shown for this timestamp,
    // and inactivity will actually cause logout before total session length does
    const lastShownInactivity = sessionStorage.getItem(
      this.INACTIVITY_WARNING_SHOWN_KEY,
    )
    if (
      data.inactivityTimeout < data.totalTimeout &&
      data.inactivityWarning <= now &&
      lastShownInactivity !== data.inactivityWarning.toString()
    ) {
      this.setWarningModalVisible(
        WarningType.INACTIVITY,
        true,
        data.inactivityWarning,
      )
      return
    }

    // Show total length warning if threshold passed and not already shown for this session
    const lastShownTotal = sessionStorage.getItem(this.TOTAL_WARNING_SHOWN_KEY)
    if (
      data.totalWarning <= now &&
      lastShownTotal !== data.totalWarning.toString()
    ) {
      this.setWarningModalVisible(
        WarningType.TOTAL_LENGTH,
        true,
        data.totalWarning,
      )
      return
    }
  }

  private static pollSession() {
    const data = this.getTimeoutData()
    if (data) {
      const now = Math.floor(Date.now() / 1000)
      this.monitorSession(data, now)
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

      this.setWarningModalVisible(WarningType.INACTIVITY, false)

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
          this.setWarningModalVisible(WarningType.INACTIVITY, false)
        })
      })
    }

    // Set up handlers for session length warning modal
    const lengthModal = document.getElementById(
      `${SessionModalType.LENGTH}-modal`,
    )
    if (lengthModal) {
      // Handle login button
      const loginButton = lengthModal.querySelector('[data-modal-primary]')
      loginButton?.addEventListener('click', () => {
        this.login()
      })

      // Handle dismiss/close buttons
      const closeButtons = lengthModal.querySelectorAll(
        '[data-modal-secondary], [data-close-modal]',
      )
      closeButtons.forEach((button) => {
        button.addEventListener('click', () => {
          this.setWarningModalVisible(WarningType.TOTAL_LENGTH, false)
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
   * Shows or hides a warning modal of the specified type.
   * When showing, also records in sessionStorage that the warning was shown.
   *
   * @param type Type of warning to show (inactivity or total length)
   * @param visible Whether to show or hide the modal
   * @param warningTimestamp Timestamp to record (used to detect new sessions and session extension)
   */
  private static setWarningModalVisible(
    type: WarningType,
    visible: boolean,
    warningTimestamp?: number,
  ) {
    const modalId =
      type === WarningType.INACTIVITY
        ? `${SessionModalType.INACTIVITY}-modal`
        : `${SessionModalType.LENGTH}-modal`
    const modal = document.getElementById(modalId)
    modal?.classList.toggle('is-hidden', !visible)

    if (type === WarningType.INACTIVITY) {
      this.inactivityWarningVisible = visible
      if (visible && warningTimestamp !== undefined) {
        sessionStorage.setItem(
          this.INACTIVITY_WARNING_SHOWN_KEY,
          warningTimestamp.toString(),
        )
      }
    } else {
      this.totalLengthWarningVisible = visible
      if (visible && warningTimestamp !== undefined) {
        sessionStorage.setItem(
          this.TOTAL_WARNING_SHOWN_KEY,
          warningTimestamp.toString(),
        )
      }
    }
  }

  /**
   * Initiates logout.
   */
  private static logout() {
    window.location.href = '/logout'
  }

  /**
   * Initiates login.
   */
  private static login() {
    window.location.href = '/logBackIn'
  }
}
