import {expect, describe, it, beforeEach, afterEach, vi, Mock} from 'vitest'
import {SessionTimeoutHandler} from '@/session'
import {ToastController} from '@/toast'
import {WarningType} from '@/session'

type SessionTimeoutHandlerType = typeof SessionTimeoutHandler & {
  logout: () => void
  pollSession: () => void
  setWarningModalVisible: (type: WarningType) => void
  inactivityWarningShown: boolean
  totalLengthWarningShown: boolean
  isInitialized: boolean
}

describe('SessionTimeoutHandler', () => {
  let container: HTMLElement
  let inactivityModal: HTMLElement
  let lengthModal: HTMLElement
  let extendSessionForm: HTMLFormElement
  let showToastSpy: ReturnType<typeof vi.spyOn>

  /**
   * Create inactivity warning modal with new structure
   */
  function createInactivityModal() {
    inactivityModal = document.createElement('div')
    inactivityModal.id = 'session-inactivity-warning-modal'
    inactivityModal.classList.add('is-hidden', 'usa-modal')
    inactivityModal.setAttribute(
      'data-modal-type',
      'session-inactivity-warning',
    )

    createExtendSessionForm()
    addSecondaryButton(inactivityModal, 'session-inactivity-warning')
    addCloseButton(inactivityModal)
    container.appendChild(inactivityModal)
  }

  /**
   * Create extend session form
   */
  function createExtendSessionForm() {
    extendSessionForm = document.createElement('form')
    extendSessionForm.id = 'extend-session-form'
    extendSessionForm.setAttribute('hx-post', '/extend-session')
    extendSessionForm.setAttribute('hx-target', 'this')
    extendSessionForm.setAttribute('hx-swap', 'none')
    addCsrfToken()
    addExtendSessionButton()
    inactivityModal.appendChild(extendSessionForm)
  }

  /**
   * Creates modal container that holds all session timeout related modals
   */
  function createModalContainer() {
    container = document.createElement('div')
    container.id = 'session-timeout-modals'
    document.body.appendChild(container)
  }

  /**
   * Creates localized message elements for session timeout notifications
   */
  function createMessageContainer() {
    const messageContainer = document.createElement('div')
    messageContainer.id = 'session-timeout-messages'
    messageContainer.classList.add('is-hidden')

    const successText = document.createElement('span')
    successText.id = 'session-extended-success-text'
    successText.textContent = 'Session successfully extended'
    messageContainer.appendChild(successText)

    const errorText = document.createElement('span')
    errorText.id = 'session-extended-error-text'
    errorText.textContent = 'Failed to extend session'
    messageContainer.appendChild(errorText)
    container.appendChild(messageContainer)
  }

  /**
   * Creates session length warning modal with buttons
   */
  function createLengthWarningModal() {
    lengthModal = document.createElement('div')
    lengthModal.id = 'session-length-warning-modal'
    lengthModal.classList.add('is-hidden', 'usa-modal')
    lengthModal.setAttribute('data-modal-type', 'session-length-warning')

    // Create primary button (logout)
    const logoutButton = document.createElement('button')
    logoutButton.textContent = 'Logout'
    logoutButton.classList.add('usa-button')
    logoutButton.setAttribute('data-modal-primary', '')
    logoutButton.setAttribute('data-modal-type', 'session-length-warning')
    lengthModal.appendChild(logoutButton)

    // Create secondary button (cancel)
    const cancelButton = document.createElement('button')
    cancelButton.textContent = 'Cancel'
    cancelButton.classList.add('usa-button', 'usa-button--unstyled')
    cancelButton.setAttribute('data-modal-secondary', '')
    cancelButton.setAttribute('data-modal-type', 'session-length-warning')
    lengthModal.appendChild(cancelButton)
    container.appendChild(lengthModal)
  }

  /**
   * Adds CSRF token input to a form
   */
  function addCsrfToken() {
    const csrfInput = document.createElement('input')
    csrfInput.setAttribute('name', 'csrfToken')
    csrfInput.value = 'test-csrf-token'
    extendSessionForm.appendChild(csrfInput)
  }

  /**
   * Adds extend session button to a form
   */
  function addExtendSessionButton() {
    const primaryButton = document.createElement('button')
    primaryButton.textContent = 'Extend Session'
    primaryButton.classList.add('usa-button')
    primaryButton.setAttribute('data-modal-primary', '')
    primaryButton.setAttribute('data-modal-type', 'session-inactivity-warning')
    extendSessionForm.appendChild(primaryButton)
  }

  /**
   * Adds secondary (cancel) button to a modal
   * @param modal Modal element to add button to
   * @param modalType Type of modal (for data attribute)
   */
  function addSecondaryButton(modal: HTMLElement, modalType: string) {
    const button = document.createElement('button')
    button.textContent = 'Cancel'
    button.classList.add('usa-button', 'usa-button--unstyled')
    button.setAttribute('data-modal-secondary', '')
    button.setAttribute('data-modal-type', modalType)
    modal.appendChild(button)
  }

  /**
   * Adds close button to a modal
   * @param modal Modal element to add button to
   */
  function addCloseButton(modal: HTMLElement) {
    const closeButton = document.createElement('button')
    closeButton.textContent = 'Close'
    closeButton.classList.add('usa-button', 'usa-modal__close')
    closeButton.setAttribute('data-close-modal', '')
    closeButton.setAttribute('data-modal-type', 'session-inactivity-warning')
    modal.appendChild(closeButton)
  }

  /**
   * Sets up all DOM elements needed for testing
   */
  function setupDomElements() {
    createModalContainer()
    createInactivityModal()
    createLengthWarningModal()
    createMessageContainer()
  }

  /**
   * Sets up all test mocks
   */
  function setupMocks() {
    // Mock ToastController
    showToastSpy = vi
      .spyOn(ToastController, 'showToastMessage')
      .mockImplementation(() => {})

    // Mock window.location
    Object.defineProperty(window, 'location', {
      value: {href: ''},
      writable: true,
    })

    const sessionStorageMock = (() => {
      let store: Record<string, string> = {}
      return {
        getItem: (key: string) => store[key] || null,
        setItem: (key: string, value: string) => {
          store[key] = value
        },
        removeItem: (key: string) => {
          delete store[key]
        },
        clear: () => {
          store = {}
        },
      }
    })()
    Object.defineProperty(window, 'sessionStorage', {
      value: sessionStorageMock,
      writable: true,
    })
  }

  beforeEach(() => {
    setupDomElements()
    setupMocks()
    // Reset initialization flag
    SessionTimeoutHandler['isInitialized'] = false
  })

  afterEach(() => {
    container.remove()

    vi.clearAllMocks()

    sessionStorage.clear()

    // Reset all static flags
    SessionTimeoutHandler['inactivityWarningVisible'] = false
    SessionTimeoutHandler['totalLengthWarningVisible'] = false
    SessionTimeoutHandler['isInitialized'] = false
  })

  describe('setWarningModalVisible', () => {
    it('shows inactivity warning modal', () => {
      SessionTimeoutHandler['setWarningModalVisible'](
        WarningType.INACTIVITY,
        true,
      )

      expect(inactivityModal.classList.contains('is-hidden')).toBe(false)
    })

    it('shows total length warning modal', () => {
      SessionTimeoutHandler['setWarningModalVisible'](
        WarningType.TOTAL_LENGTH,
        true,
      )

      expect(lengthModal.classList.contains('is-hidden')).toBe(false)
    })
  })

  describe('monitorSession', () => {
    it('shows inactivity warning and sets flags when time has passed', () => {
      const now = Math.floor(Date.now() / 1000)
      const data = {
        inactivityWarning: now - 60,
        inactivityTimeout: now + 60,
        totalWarning: now + 3600,
        totalTimeout: now + 7200,
        currentTime: now,
      }

      SessionTimeoutHandler['monitorSession'](data, now)

      expect(inactivityModal.classList.contains('is-hidden')).toBe(false)
      expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(true)
      expect(SessionTimeoutHandler['totalLengthWarningVisible']).toBe(false)
    })

    it('shows total length warning and sets flags when time has passed', () => {
      const now = Math.floor(Date.now() / 1000)
      const data = {
        inactivityWarning: now + 3600,
        inactivityTimeout: now + 7200,
        totalWarning: now - 60,
        totalTimeout: now + 60,
        currentTime: now,
      }

      SessionTimeoutHandler['monitorSession'](data, now)

      expect(lengthModal.classList.contains('is-hidden')).toBe(false)
      expect(SessionTimeoutHandler['totalLengthWarningVisible']).toBe(true)
    })

    it('does not show inactivity warning if already shown', () => {
      SessionTimeoutHandler['inactivityWarningVisible'] = true
      inactivityModal.classList.add('is-hidden')

      const now = Math.floor(Date.now() / 1000)
      const data = {
        inactivityWarning: now - 60,
        inactivityTimeout: now + 60,
        totalWarning: now + 3600,
        totalTimeout: now + 7200,
        currentTime: now,
      }

      SessionTimeoutHandler['monitorSession'](data, now)

      expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
    })

    it('does not show total length warning if already shown', () => {
      SessionTimeoutHandler['totalLengthWarningVisible'] = true
      lengthModal.classList.add('is-hidden')

      const now = Math.floor(Date.now() / 1000)
      const data = {
        inactivityWarning: now + 3600,
        inactivityTimeout: now + 7200,
        totalWarning: now - 60,
        totalTimeout: now + 60,
        currentTime: now,
      }

      SessionTimeoutHandler['monitorSession'](data, now)

      expect(lengthModal.classList.contains('is-hidden')).toBe(true)
    })

    it('does not show inactivity warning again after dismissal with same timestamp', () => {
      const now = Math.floor(Date.now() / 1000)
      const warningTimestamp = now - 60
      const data = {
        inactivityWarning: warningTimestamp,
        inactivityTimeout: now + 60,
        totalWarning: now + 3600,
        totalTimeout: now + 7200,
        currentTime: now,
      }

      // First call shows the warning
      SessionTimeoutHandler['monitorSession'](data, now)
      expect(inactivityModal.classList.contains('is-hidden')).toBe(false)
      expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(true)

      // User dismisses the modal
      SessionTimeoutHandler['setWarningModalVisible'](
        WarningType.INACTIVITY,
        false,
      )
      expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
      expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(false)

      // Second call should NOT show the warning again (same timestamp)
      SessionTimeoutHandler['monitorSession'](data, now)
      expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
      expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(false)
    })

    it('does not show total length warning again after dismissal', () => {
      const now = Math.floor(Date.now() / 1000)
      const warningTimestamp = now - 60
      const data = {
        inactivityWarning: now + 3600,
        inactivityTimeout: now + 7200,
        totalWarning: warningTimestamp,
        totalTimeout: now + 60,
        currentTime: now,
      }

      // First call shows the warning
      SessionTimeoutHandler['monitorSession'](data, now)
      expect(lengthModal.classList.contains('is-hidden')).toBe(false)
      expect(SessionTimeoutHandler['totalLengthWarningVisible']).toBe(true)

      // User dismisses the modal
      SessionTimeoutHandler['setWarningModalVisible'](
        WarningType.TOTAL_LENGTH,
        false,
      )
      expect(lengthModal.classList.contains('is-hidden')).toBe(true)
      expect(SessionTimeoutHandler['totalLengthWarningVisible']).toBe(false)

      // Second call should NOT show the warning again (same timestamp)
      SessionTimeoutHandler['monitorSession'](data, now)
      expect(lengthModal.classList.contains('is-hidden')).toBe(true)
      expect(SessionTimeoutHandler['totalLengthWarningVisible']).toBe(false)
    })

    it('shows warning again after session extension with new timestamp', () => {
      const now = Math.floor(Date.now() / 1000)
      const oldWarningTimestamp = now - 60
      const newWarningTimestamp = now + 240 // New timestamp after session extension

      const oldData = {
        inactivityWarning: oldWarningTimestamp,
        inactivityTimeout: now + 60,
        totalWarning: now + 3600,
        totalTimeout: now + 7200,
        currentTime: now,
      }

      // First call shows the warning with old timestamp
      SessionTimeoutHandler['monitorSession'](oldData, now)
      expect(inactivityModal.classList.contains('is-hidden')).toBe(false)

      // User dismisses and extends session
      SessionTimeoutHandler['setWarningModalVisible'](
        WarningType.INACTIVITY,
        false,
      )

      // Simulate time passing and new warning threshold being reached
      const laterNow = now + 300
      const newData = {
        inactivityWarning: newWarningTimestamp, // New timestamp from extended session
        inactivityTimeout: laterNow + 60,
        totalWarning: laterNow + 3600,
        totalTimeout: laterNow + 7200,
        currentTime: laterNow,
      }

      // Warning should show again because timestamp is different
      SessionTimeoutHandler['monitorSession'](newData, laterNow)
      expect(inactivityModal.classList.contains('is-hidden')).toBe(false)
      expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(true)
    })

    it('persists shown state across simulated page navigations', () => {
      const now = Math.floor(Date.now() / 1000)
      const warningTimestamp = now - 60
      const data = {
        inactivityWarning: warningTimestamp,
        inactivityTimeout: now + 60,
        totalWarning: now + 3600,
        totalTimeout: now + 7200,
        currentTime: now,
      }

      // First call shows the warning
      SessionTimeoutHandler['monitorSession'](data, now)
      expect(inactivityModal.classList.contains('is-hidden')).toBe(false)

      // User dismisses the modal
      SessionTimeoutHandler['setWarningModalVisible'](
        WarningType.INACTIVITY,
        false,
      )

      // Simulate page navigation by resetting the visible flag (as would happen on new page load)
      SessionTimeoutHandler['inactivityWarningVisible'] = false
      inactivityModal.classList.add('is-hidden')

      // Warning should NOT show again because sessionStorage remembers the timestamp
      SessionTimeoutHandler['monitorSession'](data, now)
      expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
      expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(false)
    })

    describe('setupModalEventHandlers', () => {
      it('handles successful session extension', () => {
        SessionTimeoutHandler.init()

        // Create a mock HTMX event
        const successEvent = new CustomEvent('htmx:afterRequest', {
          detail: {
            xhr: {status: 200},
            elt: extendSessionForm,
          },
        })

        document.dispatchEvent(successEvent)

        expect(inactivityModal.classList.contains('is-hidden')).toBe(true)

        expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(false)

        expect(showToastSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            id: 'session-extended-toast',
            type: 'success',
          }),
        )
      })

      it('handles failed session extension', () => {
        SessionTimeoutHandler.init()

        // Create a mock HTMX event
        const failEvent = new CustomEvent('htmx:afterRequest', {
          detail: {
            xhr: {status: 500},
            elt: extendSessionForm,
          },
        })

        document.dispatchEvent(failEvent)

        expect(inactivityModal.classList.contains('is-hidden')).toBe(true)

        expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(false)

        expect(showToastSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            id: 'session-extend-error-toast',
            type: 'error',
          }),
        )
      })

      it('handles logout button click', () => {
        SessionTimeoutHandler.init()

        const logoutButton = lengthModal.querySelector(
          '[data-modal-primary][data-modal-type="session-length-warning"]',
        ) as HTMLButtonElement
        logoutButton?.click()

        expect(window.location.href).toBe('/logout')
      })

      it('handles inactivity cancel button click', () => {
        SessionTimeoutHandler.init()
        SessionTimeoutHandler['inactivityWarningVisible'] = true
        inactivityModal.classList.remove('is-hidden')

        const cancelButton = inactivityModal.querySelector(
          '[data-modal-secondary][data-modal-type="session-inactivity-warning"]',
        ) as HTMLButtonElement
        cancelButton?.click()

        expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
        expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(false)
      })

      it('handles length cancel button click', () => {
        SessionTimeoutHandler.init()
        SessionTimeoutHandler['totalLengthWarningVisible'] = true
        lengthModal.classList.remove('is-hidden')

        const cancelButton = lengthModal.querySelector(
          '[data-modal-secondary][data-modal-type="session-length-warning"]',
        ) as HTMLButtonElement
        cancelButton?.click()

        expect(lengthModal.classList.contains('is-hidden')).toBe(true)
        expect(SessionTimeoutHandler['totalLengthWarningVisible']).toBe(false)
      })
    })

    describe('Modal Event Handlers', () => {
      let originalLocation: Location

      beforeEach(() => {
        // Store original location
        originalLocation = window.location

        // Mock window.location
        Object.defineProperty(window, 'location', {
          configurable: true,
          enumerable: true,
          value: {href: ''},
        })
      })

      afterEach(() => {
        // Restore original location
        Object.defineProperty(window, 'location', {
          configurable: true,
          enumerable: true,
          value: originalLocation,
        })
      })

      it('handles primary button click for inactivity warning', () => {
        SessionTimeoutHandler.init()
        const primaryButton = inactivityModal.querySelector(
          '[data-modal-primary][data-modal-type="session-inactivity-warning"]',
        ) as HTMLButtonElement
        const requestSubmitSpy = vi.spyOn(extendSessionForm, 'requestSubmit')

        primaryButton?.click()
        expect(requestSubmitSpy).toHaveBeenCalled()
      })

      it('handles primary button click for session length warning', () => {
        const logoutSpy = vi.spyOn(
          SessionTimeoutHandler as SessionTimeoutHandlerType,
          'logout',
        )
        SessionTimeoutHandler.init()

        const primaryButton = lengthModal.querySelector(
          '[data-modal-primary][data-modal-type="session-length-warning"]',
        ) as HTMLButtonElement
        primaryButton?.click()

        expect(logoutSpy).toHaveBeenCalled()
      })

      it('handles secondary button click', () => {
        SessionTimeoutHandler.init()
        const secondaryButton = inactivityModal.querySelector(
          '[data-modal-secondary][data-modal-type="session-inactivity-warning"]',
        ) as HTMLButtonElement
        secondaryButton?.click()

        expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
        expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(false)
      })

      it('handles close button click', () => {
        SessionTimeoutHandler.init()
        const closeButton = inactivityModal.querySelector(
          '[data-close-modal][data-modal-type="session-inactivity-warning"]',
        ) as HTMLButtonElement
        closeButton?.click()

        expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
        expect(SessionTimeoutHandler['inactivityWarningVisible']).toBe(false)
      })

      it('shows localized success message on successful session extension', () => {
        SessionTimeoutHandler.init()
        const successEvent = new CustomEvent('htmx:afterRequest', {
          detail: {
            xhr: {status: 200},
            elt: extendSessionForm,
          },
        })

        document.dispatchEvent(successEvent)

        expect(showToastSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            content: 'Session successfully extended',
            type: 'success',
          }),
        )
      })

      it('calls logout when handling timeout', () => {
        const logoutSpy = vi.spyOn(
          SessionTimeoutHandler as SessionTimeoutHandlerType,
          'logout',
        )
        SessionTimeoutHandler.init()

        const expiredTimeoutData = {
          inactivityWarning: 0,
          inactivityTimeout: 0,
          totalWarning: 0,
          totalTimeout: 0,
          currentTime: 0,
        }
        document.cookie = `session_timeout_data=${btoa(
          JSON.stringify(expiredTimeoutData),
        )}`

        SessionTimeoutHandler['pollSession']()

        expect(logoutSpy).toHaveBeenCalled()
      })
    })

    describe('pollSession', () => {
      beforeEach(() => {
        vi.useFakeTimers()
        // Reset static flags
        SessionTimeoutHandler['inactivityWarningVisible'] = false
        SessionTimeoutHandler['totalLengthWarningVisible'] = false
        SessionTimeoutHandler['isInitialized'] = false
      })

      afterEach(() => {
        vi.useRealTimers()
        document.cookie = `${SessionTimeoutHandler['TIMEOUT_COOKIE_NAME']}=; expires=Thu, 01 Jan 1970 00:00:00 GMT`
      })

      it('immediately logs out if timeout is reached', () => {
        const logoutSpy = vi.spyOn(
          SessionTimeoutHandler as SessionTimeoutHandlerType,
          'logout',
        )
        const now = Math.floor(Date.now() / 1000)

        document.cookie = `session_timeout_data=${btoa(
          JSON.stringify({
            inactivityWarning: now - 120,
            inactivityTimeout: now - 60, // Past timeout
            totalWarning: now + 3600,
            totalTimeout: now + 7200,
            currentTime: now,
          }),
        )}`

        SessionTimeoutHandler['pollSession']()
        expect(logoutSpy).toHaveBeenCalled()
      })

      it('shows inactivity warning immediately if time has passed and not shown before', () => {
        const setWarningModalVisibleSpy = vi.spyOn(
          SessionTimeoutHandler as SessionTimeoutHandlerType,
          'setWarningModalVisible',
        ) as Mock<(type: WarningType) => void>

        const now = Math.floor(Date.now() / 1000)

        document.cookie = `session_timeout_data=${btoa(
          JSON.stringify({
            inactivityWarning: now - 60, // Past warning
            inactivityTimeout: now + 60,
            totalWarning: now + 3600,
            totalTimeout: now + 7200,
            currentTime: now,
          }),
        )}`

        SessionTimeoutHandler['pollSession']()
        expect(setWarningModalVisibleSpy).toHaveBeenCalledWith(
          WarningType.INACTIVITY,
          true,
          expect.any(Number),
        )
      })

      it('shows total length warning if time has passed and no other warning shown', () => {
        const setWarningModalVisibleSpy = vi.spyOn(
          SessionTimeoutHandler as SessionTimeoutHandlerType,
          'setWarningModalVisible',
        ) as Mock<(type: WarningType) => void>

        const now = Math.floor(Date.now() / 1000)

        document.cookie = `session_timeout_data=${btoa(
          JSON.stringify({
            inactivityWarning: now + 3600,
            inactivityTimeout: now + 7200,
            totalWarning: now - 60, // Past warning
            totalTimeout: now + 3600,
            currentTime: now,
          }),
        )}`

        SessionTimeoutHandler['pollSession']()
        expect(setWarningModalVisibleSpy).toHaveBeenCalledWith(
          WarningType.TOTAL_LENGTH,
          true,
        )
      })

      it('does not show total length warning if inactivity warning is showing', () => {
        const setWarningModalVisibleSpy = vi.spyOn(
          SessionTimeoutHandler as SessionTimeoutHandlerType,
          'setWarningModalVisible',
        )
        const now = Math.floor(Date.now() / 1000)

        // Set inactivity warning as already visible
        SessionTimeoutHandler['inactivityWarningVisible'] = true

        document.cookie = `session_timeout_data=${btoa(
          JSON.stringify({
            inactivityWarning: now - 60,
            inactivityTimeout: now + 60,
            totalWarning: now - 30, // Past warning
            totalTimeout: now + 3600,
            currentTime: now,
          }),
        )}`

        SessionTimeoutHandler['pollSession']()
        expect(setWarningModalVisibleSpy).not.toHaveBeenCalled()
      })

      it('sets timer for future inactivity warning', () => {
        const setWarningModalVisibleSpy = vi.spyOn(
          SessionTimeoutHandler as SessionTimeoutHandlerType,
          'setWarningModalVisible',
        ) as Mock<(type: WarningType) => void>

        const now = Math.floor(Date.now() / 1000)

        document.cookie = `session_timeout_data=${btoa(
          JSON.stringify({
            inactivityWarning: now + 60, // Future warning
            inactivityTimeout: now + 120,
            totalWarning: now + 3600,
            totalTimeout: now + 7200,
            currentTime: now,
          }),
        )}`

        SessionTimeoutHandler['pollSession']()

        // Fast forward to warning time
        vi.advanceTimersByTime(60000)
        expect(setWarningModalVisibleSpy).toHaveBeenCalledWith(
          WarningType.INACTIVITY,
          true,
          expect.any(Number),
        )
      })
    })

    describe('init', () => {
      it('initializes only once', () => {
        const pollSessionSpy = vi.spyOn(
          SessionTimeoutHandler as SessionTimeoutHandlerType,
          'pollSession',
        )

        // First initialization
        SessionTimeoutHandler.init()
        expect(pollSessionSpy).toHaveBeenCalledTimes(1)
        expect(SessionTimeoutHandler['isInitialized']).toBe(true)

        // Second initialization attempt
        SessionTimeoutHandler.init()

        // Still only called once
        expect(pollSessionSpy).toHaveBeenCalledTimes(1)
      })
    })
  })

  describe('init', () => {
    it('initializes only once', () => {
      const checkAndSetTimerSpy = jest.spyOn(
        SessionTimeoutHandler as SessionTimeoutHandlerType,
        'checkAndSetTimer',
      )

      // First initialization
      SessionTimeoutHandler.init()
      expect(checkAndSetTimerSpy).toHaveBeenCalledTimes(1)
      expect(SessionTimeoutHandler['isInitialized']).toBe(true)

      // Second initialization attempt
      SessionTimeoutHandler.init()

      // Still only called once
      expect(checkAndSetTimerSpy).toHaveBeenCalledTimes(1)
    })
  })
})
