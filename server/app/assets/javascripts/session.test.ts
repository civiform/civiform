import {jest, expect, describe, it, beforeEach, afterEach} from '@jest/globals'
import {SessionTimeoutHandler} from './session'
import {ToastController} from './toast'
import {WarningType} from './session'

type SessionTimeoutHandlerType = typeof SessionTimeoutHandler & {
  logout: () => void
  checkAndSetTimer: () => void
  showWarning: (type: WarningType) => void
  inactivityWarningShown: boolean
  totalLengthWarningShown: boolean
}

describe('SessionTimeoutHandler', () => {
  let container: HTMLElement
  let inactivityModal: HTMLElement
  let lengthModal: HTMLElement
  let extendSessionForm: HTMLFormElement
  let consoleSpy: ReturnType<typeof jest.spyOn>

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
    jest.spyOn(ToastController, 'showToastMessage').mockImplementation(() => {})

    // Mock console.error
    consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {})

    // Mock window.location
    Object.defineProperty(window, 'location', {
      value: {href: ''},
      writable: true,
    })
  }

  beforeEach(() => {
    setupDomElements()
    setupMocks()
  })

  afterEach(() => {
    container.remove()

    jest.clearAllMocks()

    SessionTimeoutHandler['inactivityWarningShown'] = false
    SessionTimeoutHandler['totalLengthWarningShown'] = false
  })

  describe('showWarning', () => {
    it('shows inactivity warning modal', () => {
      SessionTimeoutHandler['showWarning'](WarningType.INACTIVITY)

      expect(inactivityModal.classList.contains('is-hidden')).toBe(false)

      expect(SessionTimeoutHandler['inactivityWarningShown']).toBe(true)
    })

    it('shows total length warning modal', () => {
      SessionTimeoutHandler['showWarning'](WarningType.TOTAL_LENGTH)

      expect(lengthModal.classList.contains('is-hidden')).toBe(false)

      expect(SessionTimeoutHandler['totalLengthWarningShown']).toBe(true)
    })

    it('does not show inactivity warning modal if already shown', () => {
      SessionTimeoutHandler['inactivityWarningShown'] = true

      inactivityModal.classList.add('is-hidden')

      SessionTimeoutHandler['showWarning'](WarningType.INACTIVITY)

      expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
    })

    it('does not show total length warning modal if already shown', () => {
      SessionTimeoutHandler['totalLengthWarningShown'] = true

      lengthModal.classList.add('is-hidden')

      SessionTimeoutHandler['showWarning'](WarningType.TOTAL_LENGTH)

      expect(lengthModal.classList.contains('is-hidden')).toBe(true)
    })

    it('logs error if modal element is not found', () => {
      inactivityModal.remove()

      SessionTimeoutHandler['showWarning'](WarningType.INACTIVITY)

      expect(consoleSpy).toHaveBeenCalledWith(
        'Modal with ID session-inactivity-warning-modal not found',
      )
    })
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

      expect(SessionTimeoutHandler['inactivityWarningShown']).toBe(false)

      expect(ToastController.showToastMessage).toHaveBeenCalledWith(
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

      expect(SessionTimeoutHandler['inactivityWarningShown']).toBe(false)

      expect(ToastController.showToastMessage).toHaveBeenCalledWith(
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
      SessionTimeoutHandler['inactivityWarningShown'] = true
      inactivityModal.classList.remove('is-hidden')

      const cancelButton = inactivityModal.querySelector(
        '[data-modal-secondary][data-modal-type="session-inactivity-warning"]',
      ) as HTMLButtonElement
      cancelButton?.click()

      expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
      expect(SessionTimeoutHandler['inactivityWarningShown']).toBe(false)
    })

    it('handles length cancel button click', () => {
      SessionTimeoutHandler.init()
      SessionTimeoutHandler['totalLengthWarningShown'] = true
      lengthModal.classList.remove('is-hidden')

      const cancelButton = lengthModal.querySelector(
        '[data-modal-secondary][data-modal-type="session-length-warning"]',
      ) as HTMLButtonElement
      cancelButton?.click()

      expect(lengthModal.classList.contains('is-hidden')).toBe(true)
      expect(SessionTimeoutHandler['totalLengthWarningShown']).toBe(false)
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
      const requestSubmitSpy = jest.spyOn(extendSessionForm, 'requestSubmit')

      primaryButton?.click()
      expect(requestSubmitSpy).toHaveBeenCalled()
    })

    it('handles primary button click for session length warning', () => {
      const logoutSpy = jest.spyOn(
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
      expect(SessionTimeoutHandler['inactivityWarningShown']).toBe(false)
    })

    it('handles close button click', () => {
      SessionTimeoutHandler.init()
      const closeButton = inactivityModal.querySelector(
        '[data-close-modal][data-modal-type="session-inactivity-warning"]',
      ) as HTMLButtonElement
      closeButton?.click()

      expect(inactivityModal.classList.contains('is-hidden')).toBe(true)
      expect(SessionTimeoutHandler['inactivityWarningShown']).toBe(false)
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

      expect(ToastController.showToastMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          content: 'Session successfully extended',
          type: 'success',
        }),
      )
    })

    it('calls logout when handling timeout', () => {
      const logoutSpy = jest.spyOn(
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
      document.cookie = `session_timeout_data=${btoa(JSON.stringify(expiredTimeoutData))}`

      SessionTimeoutHandler['checkAndSetTimer']()

      expect(logoutSpy).toHaveBeenCalled()
    })
  })

  describe('checkAndSetTimer', () => {
    beforeEach(() => {
      jest.useFakeTimers()
      // Reset static flags
      SessionTimeoutHandler['hasInactivityWarningBeenShown'] = false
      SessionTimeoutHandler['hasTotalLengthWarningBeenShown'] = false
      SessionTimeoutHandler['inactivityWarningShown'] = false
      SessionTimeoutHandler['totalLengthWarningShown'] = false
      SessionTimeoutHandler['timer'] = null
    })

    afterEach(() => {
      jest.useRealTimers()
      document.cookie = `${SessionTimeoutHandler['TIMEOUT_COOKIE_NAME']}=; expires=Thu, 01 Jan 1970 00:00:00 GMT`
    })

    it('immediately logs out if timeout is reached', () => {
      const logoutSpy = jest.spyOn(
        SessionTimeoutHandler as SessionTimeoutHandlerType,
        'handleTimeout',
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

      SessionTimeoutHandler['checkAndSetTimer']()
      expect(logoutSpy).toHaveBeenCalled()
    })

    it('shows inactivity warning immediately if time has passed and not shown before', () => {
      const showWarningSpy = jest.spyOn(
        SessionTimeoutHandler as SessionTimeoutHandlerType,
        'showWarning',
      )
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

      SessionTimeoutHandler['checkAndSetTimer']()
      expect(showWarningSpy).toHaveBeenCalledWith(WarningType.INACTIVITY)
      expect(SessionTimeoutHandler['hasInactivityWarningBeenShown']).toBe(true)
    })

    it('does not show inactivity warning if already shown before', () => {
      const showWarningSpy = jest.spyOn(
        SessionTimeoutHandler as SessionTimeoutHandlerType,
        'showWarning',
      )
      const now = Math.floor(Date.now() / 1000)

      SessionTimeoutHandler['hasInactivityWarningBeenShown'] = true

      document.cookie = `session_timeout_data=${btoa(
        JSON.stringify({
          inactivityWarning: now - 60,
          inactivityTimeout: now + 60,
          totalWarning: now + 3600,
          totalTimeout: now + 7200,
          currentTime: now,
        }),
      )}`

      SessionTimeoutHandler['checkAndSetTimer']()
      expect(showWarningSpy).not.toHaveBeenCalled()
    })

    it('shows total length warning if time has passed and no other warning shown', () => {
      const showWarningSpy = jest.spyOn(
        SessionTimeoutHandler as SessionTimeoutHandlerType,
        'showWarning',
      )
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

      SessionTimeoutHandler['checkAndSetTimer']()
      expect(showWarningSpy).toHaveBeenCalledWith(WarningType.TOTAL_LENGTH)
      expect(SessionTimeoutHandler['hasTotalLengthWarningBeenShown']).toBe(true)
    })

    it('does not show total length warning if inactivity warning is showing', () => {
      const showWarningSpy = jest.spyOn(
        SessionTimeoutHandler as SessionTimeoutHandlerType,
        'showWarning',
      )
      const now = Math.floor(Date.now() / 1000)

      SessionTimeoutHandler['inactivityWarningShown'] = true

      document.cookie = `session_timeout_data=${btoa(
        JSON.stringify({
          inactivityWarning: now - 60,
          inactivityTimeout: now + 60,
          totalWarning: now - 30, // Past warning
          totalTimeout: now + 3600,
          currentTime: now,
        }),
      )}`

      SessionTimeoutHandler['checkAndSetTimer']()
      expect(showWarningSpy).not.toHaveBeenCalled()
    })

    it('sets timer for future inactivity warning', () => {
      const showWarningSpy = jest.spyOn(
        SessionTimeoutHandler as SessionTimeoutHandlerType,
        'showWarning',
      )
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

      SessionTimeoutHandler['checkAndSetTimer']()
      expect(SessionTimeoutHandler['timer']).not.toBeNull()

      // Fast forward to warning time
      jest.advanceTimersByTime(60000)
      expect(showWarningSpy).toHaveBeenCalledWith(WarningType.INACTIVITY)
    })

    it('clears existing timer before setting new one', () => {
      const clearTimeoutSpy = jest.spyOn(window, 'clearTimeout')
      const now = Math.floor(Date.now() / 1000)

      // Set initial timer
      document.cookie = `session_timeout_data=${btoa(
        JSON.stringify({
          inactivityWarning: now + 60,
          inactivityTimeout: now + 120,
          totalWarning: now + 3600,
          totalTimeout: now + 7200,
          currentTime: now,
        }),
      )}`

      SessionTimeoutHandler['checkAndSetTimer']()
      const firstTimer = SessionTimeoutHandler['timer']
      expect(firstTimer).not.toBeNull()

      // Call again to check if timer is cleared
      SessionTimeoutHandler['checkAndSetTimer']()
      expect(clearTimeoutSpy).toHaveBeenCalledWith(firstTimer)
    })
  })
})
