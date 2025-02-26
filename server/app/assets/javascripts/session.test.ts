import {jest, expect, describe, it, beforeEach, afterEach} from '@jest/globals'
import {SessionTimeoutHandler} from './session'
import {ToastController} from './toast'
import {WarningType} from './session'

describe('SessionTimeoutHandler', () => {
  let container: HTMLElement
  let csrfInput: HTMLInputElement
  let consoleSpy: ReturnType<typeof jest.spyOn>

  beforeEach(() => {
    // Set up DOM elements
    container = document.createElement('div')
    document.body.appendChild(container)

    // Create CSRF token input
    csrfInput = document.createElement('input')
    csrfInput.setAttribute('name', 'csrfToken')
    csrfInput.value = 'test-csrf-token'
    container.appendChild(csrfInput)

    // Mock ToastController
    jest.spyOn(ToastController, 'showToastMessage').mockImplementation(() => {})
    // Mock console.error
    consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    // Clean up DOM
    container.remove()
    document.querySelectorAll('.usa-modal-wrapper').forEach((el) => el.remove())

    // Reset mocks
    jest.clearAllMocks()

    // Reset the warningShown flag again for good measure
    // @ts-expect-erro - accessing private static property for testing
    SessionTimeoutHandler['warningShown'] = false
  })

  describe('showWarning', () => {
    it('shows inactivity warning modal with extend session form', () => {
      SessionTimeoutHandler['showWarning'](WarningType.INACTIVITY)

      // Check if modal is shown
      const modal = document.querySelector('.usa-modal-wrapper')
      expect(modal).not.toBeNull()
      if (!modal) return

      // Check modal content
      expect(modal.textContent).toContain('Session Inactivity Warning')
      expect(modal.textContent).toContain(
        'Your session will expire soon due to inactivity',
      )

      // Check if form exists with correct HTMX attributes
      const form = modal.querySelector('form')
      expect(form).not.toBeNull()
      if (!form) return

      expect(form.getAttribute('hx-post')).toBe('/extend-session')
      expect(form.getAttribute('hx-swap')).toBe('none')

      // Check if CSRF token is present
      const formCsrfInput = form.querySelector(
        'input[name=csrfToken]',
      ) as HTMLInputElement
      expect(formCsrfInput).not.toBeNull()
      expect(formCsrfInput.value).toBe('test-csrf-token')
    })

    it('shows total length warning modal with logout button', () => {
      SessionTimeoutHandler['showWarning'](WarningType.TOTAL_LENGTH)

      const modal = document.querySelector('.usa-modal-wrapper')
      expect(modal).not.toBeNull()
      if (!modal) return
      expect(modal.textContent).toContain('Session Length Warning')
      expect(modal.textContent).toContain(
        'Your session will expire soon due to length',
      )

      // Check if logout button exists instead of form
      const form = modal.querySelector('form')
      expect(form).toBeNull()

      const logoutButton = modal.querySelector('button.usa-button')
      expect(logoutButton).not.toBeNull()
      if (!logoutButton) return

      expect(logoutButton.textContent).toBe('Logout')
    })

    it('closes modal when clicking outside', () => {
      SessionTimeoutHandler['showWarning'](WarningType.INACTIVITY)
      const modal = document.querySelector('.usa-modal-wrapper')
      expect(modal).not.toBeNull()
      if (!modal) return

      // Simulate click outside modal
      modal.dispatchEvent(new MouseEvent('click', {bubbles: true}))

      // Modal should be removed
      expect(document.querySelector('.usa-modal-wrapper')).toBeNull()
    })

    it('handles successful session extension', () => {
      SessionTimeoutHandler['showWarning'](WarningType.INACTIVITY)
      const form = document.querySelector('form')
      expect(form).not.toBeNull()
      if (!form) return

      // Mock successful HTMX request with just the properties we use
      const successEvent = new CustomEvent('htmx:afterRequest', {
        detail: {
          xhr: {
            status: 200,
            statusText: 'OK',
          } as XMLHttpRequest,
        },
      })

      // Get reference to modal container before removal
      const modalContainer = document.querySelector('.usa-modal')
      expect(modalContainer).not.toBeNull()
      if (!modalContainer) return

      form.dispatchEvent(successEvent)

      // Check if modal is closed
      expect(document.querySelector('.usa-modal-wrapper')).toBeNull()

      // Check if success toast is shown
      expect(ToastController.showToastMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'session-extended-toast',
          content: 'Session successfully extended',
          type: 'success',
          duration: 3000,
          canDismiss: true,
          canIgnore: false,
          condOnStorageKey: null,
        }),
      )
    })

    it('handles failed session extension', () => {
      SessionTimeoutHandler['showWarning'](WarningType.INACTIVITY)
      const form = document.querySelector('form')
      expect(form).not.toBeNull()
      if (!form) return

      // Mock failed HTMX request with just the properties we use
      const failEvent = new CustomEvent('htmx:afterRequest', {
        detail: {
          xhr: {
            status: 500,
            statusText: 'Server Error',
          } as XMLHttpRequest,
        },
      })

      // Get reference to modal container before removal
      const modalContainer = document.querySelector('.usa-modal')
      expect(modalContainer).not.toBeNull()
      if (!modalContainer) return

      form.dispatchEvent(failEvent)

      // Check if modal is closed
      expect(document.querySelector('.usa-modal-wrapper')).toBeNull()

      // Check if error toast is shown
      expect(ToastController.showToastMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'session-extend-error-toast',
          content: 'Failed to extend session',
          type: 'error',
          duration: 3000,
          canDismiss: true,
          canIgnore: false,
          condOnStorageKey: null,
        }),
      )

      // Verify console.error was called with the correct message
      expect(consoleSpy).toHaveBeenCalledWith(
        'Failed to extend session:',
        'Server Error',
      )
    })
  })
})
