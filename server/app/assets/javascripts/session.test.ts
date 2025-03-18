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

  beforeEach(() => {
    // Set up DOM elements
    container = document.createElement('div')
    container.id = 'session-timeout-modals'
    document.body.appendChild(container)

    // Create inactivity warning modal with new structure
    inactivityModal = document.createElement('div')
    inactivityModal.id = 'session-inactivity-warning-modal'
    inactivityModal.classList.add('is-hidden', 'usa-modal')
    inactivityModal.setAttribute(
      'data-modal-type',
      'session-inactivity-warning',
    )
    container.appendChild(inactivityModal)

    // Create extend session form
    extendSessionForm = document.createElement('form')
    extendSessionForm.id = 'extend-session-form'
    extendSessionForm.setAttribute('hx-post', '/extend-session')
    extendSessionForm.setAttribute('hx-target', 'this')
    extendSessionForm.setAttribute('hx-swap', 'none')

    // Add CSRF token input
    const csrfInput = document.createElement('input')
    csrfInput.setAttribute('name', 'csrfToken')
    csrfInput.value = 'test-csrf-token'
    extendSessionForm.appendChild(csrfInput)

    // Create primary button (extend session)
    const primaryButton = document.createElement('button')
    primaryButton.textContent = 'Extend Session'
    primaryButton.classList.add('usa-button')
    primaryButton.setAttribute('data-modal-primary', '')
    primaryButton.setAttribute('data-modal-type', 'session-inactivity-warning')
    extendSessionForm.appendChild(primaryButton)

    inactivityModal.appendChild(extendSessionForm)

    // Create secondary button (cancel)
    const secondaryButton = document.createElement('button')
    secondaryButton.textContent = 'Cancel'
    secondaryButton.classList.add('usa-button', 'usa-button--unstyled')
    secondaryButton.setAttribute('data-modal-secondary', '')
    secondaryButton.setAttribute(
      'data-modal-type',
      'session-inactivity-warning',
    )
    inactivityModal.appendChild(secondaryButton)

    // Create close button
    const closeButton = document.createElement('button')
    closeButton.textContent = 'Close'
    closeButton.classList.add('usa-button', 'usa-modal__close')
    closeButton.setAttribute('data-close-modal', '')
    closeButton.setAttribute('data-modal-type', 'session-inactivity-warning')
    inactivityModal.appendChild(closeButton)

    // Create session length warning modal
    lengthModal = document.createElement('div')
    lengthModal.id = 'session-length-warning-modal'
    lengthModal.classList.add('is-hidden', 'usa-modal')
    lengthModal.setAttribute('data-modal-type', 'session-length-warning')
    container.appendChild(lengthModal)

    // Create primary button (logout)
    const logoutButton = document.createElement('button')
    logoutButton.textContent = 'Logout'
    logoutButton.classList.add('usa-button')
    logoutButton.setAttribute('data-modal-primary', '')
    logoutButton.setAttribute('data-modal-type', 'session-length-warning')
    lengthModal.appendChild(logoutButton)

    // Create secondary button (cancel)
    const lengthCancelButton = document.createElement('button')
    lengthCancelButton.textContent = 'Cancel'
    lengthCancelButton.classList.add('usa-button', 'usa-button--unstyled')
    lengthCancelButton.setAttribute('data-modal-secondary', '')
    lengthCancelButton.setAttribute('data-modal-type', 'session-length-warning')
    lengthModal.appendChild(lengthCancelButton)

    // Add localized message elements
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

    // Mock ToastController
    jest.spyOn(ToastController, 'showToastMessage').mockImplementation(() => {})

    // Mock console.error
    consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {})

    // Mock window.location
    Object.defineProperty(window, 'location', {
      value: {href: ''},
      writable: true,
    })
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
})
