/* eslint-disable no-unsanitized/property */
import {describe, it, expect, beforeEach, afterEach, vi} from 'vitest'
import {Modal, type ModalActionSelectedEvent} from '@/components/shared/modal'

// Mock the USWDS modal module
vi.mock('@uswds/uswds/js/usa-modal', () => ({
  default: {
    toggleModal: vi.fn(),
    teardown: vi.fn(),
  },
}))

// Import the mocked module to get access to the mock functions
import {default as mockModal} from '@uswds/uswds/js/usa-modal'

describe('Modal', () => {
  let modalElement: Modal

  // Helper function to create a basic modal with required attributes
  const createModal = (
    options: {
      modalId?: string
      headingText?: string
      descriptionHtml?: string
      size?: string
      forceAction?: boolean
      buttons?: Array<{
        text: string
        action: string
        className?: string
        isLink?: boolean
      }>
    } = {},
  ) => {
    const {
      modalId = 'test-modal',
      headingText = 'Test Heading',
      descriptionHtml = 'Test description',
      size,
      forceAction,
      buttons,
    } = options

    const modal = document.createElement('wc-modal') as Modal
    modal.setAttribute('modal-id', modalId)
    modal.setAttribute('heading-text', headingText)

    if (size) {
      modal.setAttribute('size', size)
    }

    if (forceAction) {
      modal.setAttribute('force-action', 'true')
    }

    // Add description slot
    const description = document.createElement('div')
    description.setAttribute('slot', 'description')
    //
    description.innerHTML = descriptionHtml
    modal.appendChild(description)

    // Add buttons if provided
    if (buttons && buttons.length > 0) {
      const buttonsContainer = document.createElement('div')
      buttonsContainer.setAttribute('slot', 'buttons')

      buttons.forEach((btn) => {
        const element = btn.isLink
          ? document.createElement('a')
          : document.createElement('button')
        element.textContent = btn.text
        element.setAttribute('data-action', btn.action)
        if (btn.className) {
          element.className = btn.className
        }
        if (btn.isLink) {
          ;(element as HTMLAnchorElement).href = '#'
        }
        buttonsContainer.appendChild(element)
      })

      modal.appendChild(buttonsContainer)
    }

    return modal
  }

  // Helper function to simulate USWDS modal wrapper structure
  const createUswdsWrapper = (
    modalElement: Modal,
    options: {
      isVisible?: boolean
      includeModalDiv?: boolean
    } = {},
  ) => {
    const {isVisible = false, includeModalDiv = false} = options

    const innerModal = modalElement.querySelector('#test-modal')
    if (!innerModal) {
      throw new Error(
        'Modal element not found - did you append the modal to the DOM first?',
      )
    }

    const wrapper = document.createElement('div')
    wrapper.id = 'test-modal'
    wrapper.classList.add('usa-modal-wrapper')

    if (isVisible) {
      wrapper.classList.add('is-visible')
    }

    if (includeModalDiv) {
      const modal = document.createElement('div')
      modal.classList.add('usa-modal')
      wrapper.appendChild(modal)
    } else {
      const modalClone = innerModal.cloneNode(true)
      wrapper.appendChild(modalClone)
    }

    innerModal.id = ''
    document.body.appendChild(wrapper)

    return wrapper
  }

  beforeEach(() => {
    // Clear the DOM
    document.body.innerHTML = ''

    // Reset mocks
    vi.clearAllMocks()

    // Define custom element if not already defined
    if (!customElements.get('wc-modal')) {
      customElements.define('wc-modal', Modal)
    }

    // Create a new modal element
    modalElement = document.createElement('wc-modal') as Modal
  })

  afterEach(() => {
    if (modalElement.parentNode) {
      modalElement.remove()
    }
  })

  describe('initialization', () => {
    it('should require modal-id attribute to initialize', () => {
      // Using the helper with all defaults
      modalElement = createModal()

      expect(() => {
        document.body.appendChild(modalElement)
      }).not.toThrow()

      expect(modalElement.querySelector('.usa-modal')).toBeTruthy()
    })

    it('should require heading-text attribute to initialize', () => {
      modalElement = createModal({headingText: 'Custom Heading'})

      expect(() => {
        document.body.appendChild(modalElement)
      }).not.toThrow()

      const heading = modalElement.querySelector('.usa-modal__heading')
      expect(heading?.textContent).toBe('Custom Heading')
    })

    it('should require description slot to initialize', () => {
      modalElement = createModal({descriptionHtml: 'Custom description'})

      expect(() => {
        document.body.appendChild(modalElement)
      }).not.toThrow()

      const desc = modalElement.querySelector('#test-modal-modal-description')
      expect(desc?.textContent).toContain('Custom description')
    })

    it('should initialize correctly with all required attributes', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      expect(modalElement.querySelector('.usa-modal')).toBeTruthy()
      expect(modalElement.querySelector('#test-modal')).toBeTruthy()
      expect(
        modalElement.querySelector('.usa-modal__heading')?.textContent,
      ).toBe('Test Heading')
    })

    it('should only initialize once', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      const firstHTML = modalElement.innerHTML

      // Remove and re-append (should not re-initialize due to _initialized flag)
      document.body.removeChild(modalElement)
      document.body.appendChild(modalElement)

      expect(modalElement.innerHTML).toBe(firstHTML)
    })
  })

  describe('modal size', () => {
    it('should apply size class when size attribute is provided', () => {
      modalElement = createModal({size: 'lg'})
      document.body.appendChild(modalElement)

      expect(modalElement.querySelector('.usa-modal--lg')).toBeTruthy()
    })

    it('should not apply size class when size attribute is not provided', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      const modal = modalElement.querySelector('.usa-modal')
      expect(modal?.classList.contains('usa-modal--lg')).toBe(false)
    })
  })

  describe('force action mode', () => {
    it('should add data-force-action attribute when force-action is true', () => {
      modalElement = createModal({
        forceAction: true,
        buttons: [
          {text: 'Confirm', action: 'confirm', className: 'usa-button'},
        ],
      })
      document.body.appendChild(modalElement)

      expect(
        modalElement
          .querySelector('.usa-modal')
          ?.hasAttribute('data-force-action'),
      ).toBe(true)
    })

    it('should not render close button when force-action is true', () => {
      modalElement = createModal({
        forceAction: true,
        buttons: [{text: 'OK', action: 'ok'}],
      })
      document.body.appendChild(modalElement)

      expect(modalElement.querySelector('.usa-modal__close')).toBeNull()
    })

    it('should render close button when force-action is not set', () => {
      modalElement = createModal() // forceAction defaults to false
      document.body.appendChild(modalElement)

      expect(modalElement.querySelector('.usa-modal__close')).toBeTruthy()
    })
  })

  describe('button handling', () => {
    it('should render no footer when no buttons are provided', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      const footer = modalElement.querySelector('.usa-modal__footer')
      expect(footer).toBeNull()
    })

    it('should render buttons in footer', () => {
      modalElement = createModal({
        buttons: [
          {text: 'Confirm', action: 'confirm', className: 'usa-button'},
          {
            text: 'Cancel',
            action: 'cancel',
            className: 'usa-button usa-button--secondary',
          },
        ],
      })
      document.body.appendChild(modalElement)

      const footer = modalElement.querySelector('.usa-modal__footer')
      expect(footer).toBeTruthy()

      const buttons = footer?.querySelectorAll('button')
      expect(buttons?.length).toBe(2)
    })

    it('should add data-close-modal attribute to buttons', () => {
      modalElement = createModal({
        buttons: [{text: 'Confirm', action: 'confirm'}],
      })
      document.body.appendChild(modalElement)

      const renderedButton = modalElement.querySelector(
        '.usa-modal__footer button',
      )
      expect(renderedButton?.hasAttribute('data-close-modal')).toBe(true)
    })

    it('should preserve data-action attributes', () => {
      modalElement = createModal({
        buttons: [
          {text: 'First', action: 'first-action'},
          {text: 'Second', action: 'second-action'},
          {text: 'Third', action: 'third-action'},
        ],
      })
      document.body.appendChild(modalElement)

      const buttons = modalElement.querySelectorAll('.usa-modal__footer button')
      expect(buttons[0].getAttribute('data-action')).toBe('first-action')
      expect(buttons[1].getAttribute('data-action')).toBe('second-action')
      expect(buttons[2].getAttribute('data-action')).toBe('third-action')
    })

    it('should require data-action attribute on buttons', () => {
      // This test documents that buttons require data-action attribute
      // The component will throw an error if data-action is missing
      // However, testing thrown errors in connectedCallback is difficult in jsdom
      // So we test the happy path where data-action is present

      modalElement = createModal({
        buttons: [{text: 'Confirm', action: 'confirm'}],
      })

      // Should not throw when data-action is present
      expect(() => {
        document.body.appendChild(modalElement)
      }).not.toThrow()

      const renderedButton = modalElement.querySelector(
        '.usa-modal__footer button',
      )
      expect(renderedButton?.getAttribute('data-action')).toBe('confirm')
    })

    it('should dispatch custom event when button is clicked', () => {
      modalElement = createModal({
        buttons: [{text: 'Confirm', action: 'confirm-action'}],
      })
      document.body.appendChild(modalElement)

      const eventSpy = vi.fn()
      modalElement.addEventListener('modal:action-selected', eventSpy)

      const renderedButton = modalElement.querySelector(
        '.usa-modal__footer button',
      ) as HTMLButtonElement
      renderedButton.click()

      expect(eventSpy).toHaveBeenCalled()
      const event = eventSpy.mock
        .calls[0][0] as CustomEvent<ModalActionSelectedEvent>
      expect(event.detail.action).toBe('confirm-action')
    })

    it('should work with anchor elements as buttons', () => {
      modalElement = createModal({
        buttons: [{text: 'Link Button', action: 'link-action', isLink: true}],
      })
      document.body.appendChild(modalElement)

      const renderedLink = modalElement.querySelector('.usa-modal__footer a')
      expect(renderedLink).toBeTruthy()
      expect(renderedLink?.hasAttribute('data-close-modal')).toBe(true)
      expect(renderedLink?.getAttribute('data-action')).toBe('link-action')
    })
  })

  describe('open method', () => {
    it('should call USWDS toggleModal when opening', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      createUswdsWrapper(modalElement)

      modalElement.open()

      expect(mockModal.toggleModal).toHaveBeenCalled()
    })

    it('should not open if modal is already visible', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      createUswdsWrapper(modalElement, {isVisible: true})

      modalElement.open()

      expect(mockModal.toggleModal).not.toHaveBeenCalled()
    })

    it('should not open if modal-id is not set', () => {
      // Create modal without initializing it (no appendChild)
      modalElement.open()

      expect(mockModal.toggleModal).not.toHaveBeenCalled()
    })
  })

  describe('close method', () => {
    it('should call USWDS toggleModal when closing', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      createUswdsWrapper(modalElement, {isVisible: true, includeModalDiv: true})

      modalElement.close()

      expect(mockModal.toggleModal).toHaveBeenCalled()
    })

    it('should not close if modal is not visible', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      createUswdsWrapper(modalElement)

      modalElement.close()

      expect(mockModal.toggleModal).not.toHaveBeenCalled()
    })

    it('should not close if modal-id is not set', () => {
      // Create modal without initializing it (no appendChild)
      modalElement.close()

      expect(mockModal.toggleModal).not.toHaveBeenCalled()
    })
  })

  describe('isOpen method', () => {
    it('should return true when modal is visible', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      createUswdsWrapper(modalElement, {isVisible: true})

      expect(modalElement.isOpen()).toBe(true)
    })

    it('should return false when modal is not visible', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      createUswdsWrapper(modalElement)

      expect(modalElement.isOpen()).toBe(false)
    })

    it('should return false when modal-id is not set', () => {
      expect(modalElement.isOpen()).toBe(false)
    })
  })

  describe('toggle method', () => {
    it('should close modal when it is open', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      createUswdsWrapper(modalElement, {isVisible: true, includeModalDiv: true})

      modalElement.toggle()

      expect(mockModal.toggleModal).toHaveBeenCalled()
    })

    it('should open modal when it is closed', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      createUswdsWrapper(modalElement)

      modalElement.toggle()

      expect(mockModal.toggleModal).toHaveBeenCalled()
    })
  })

  describe('disconnectedCallback', () => {
    it('should call USWDS teardown when element is removed', async () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      // Simulate USWDS wrapper with modal element inside
      const wrapper = document.createElement('div')
      wrapper.id = 'test-modal'
      wrapper.classList.add('usa-modal-wrapper')

      const modal = document.createElement('div')
      modal.classList.add('usa-modal')
      wrapper.appendChild(modal)

      document.body.appendChild(wrapper)

      // In test environments, disconnectedCallback might not trigger automatically
      // So we remove the element and manually trigger the callback
      modalElement.remove()

      // Wait a tick for any async operations
      await new Promise((resolve) => setTimeout(resolve, 0))

      // Manually trigger disconnectedCallback since jsdom doesn't always call it
      // Use bind to maintain proper `this` context
      const boundDisconnect =
        modalElement.disconnectedCallback.bind(modalElement)
      boundDisconnect()

      expect(mockModal.teardown).toHaveBeenCalled()
    })

    it('should not error if modal-id is not set', () => {
      // Create modal without initializing
      expect(() =>
        modalElement.disconnectedCallback.bind(modalElement)(),
      ).not.toThrow()
    })

    it('should not error if modal element is not found', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      expect(() =>
        modalElement.disconnectedCallback.bind(modalElement)(),
      ).not.toThrow()
    })
  })

  describe('security and sanitization', () => {
    it('should preserve safe HTML in description', () => {
      modalElement = createModal({
        descriptionHtml: 'Text with <strong>bold</strong> and <em>italic</em>',
      })
      document.body.appendChild(modalElement)

      const descElement = modalElement.querySelector(
        '#test-modal-modal-description',
      )
      expect(descElement?.innerHTML).toContain('<strong>bold</strong>')
      expect(descElement?.innerHTML).toContain('<em>italic</em>')
    })

    it('should sanitize the entire innerHTML assignment', () => {
      modalElement = createModal({
        headingText: 'Heading <img src=x onerror=alert(1)>',
        descriptionHtml: 'Description <img src=x onerror=alert(2)>',
        buttons: [{text: 'Button', action: 'test'}],
      })

      // Manually set malicious button HTML
      const buttonSlot = modalElement.querySelector('[slot="buttons"]')
      const button = buttonSlot?.querySelector('button')
      if (button) {
        button.innerHTML = 'Button <img src=x onerror=alert(3)>'
      }

      document.body.appendChild(modalElement)

      // No onerror handlers should exist anywhere
      expect(modalElement.innerHTML).not.toContain('onerror')
      expect(modalElement.innerHTML).not.toContain('alert(1)')
      expect(modalElement.innerHTML).not.toContain('alert(2)')
      expect(modalElement.innerHTML).not.toContain('alert(3)')
    })

    it('should validate modal size attribute', () => {
      // Test valid size
      modalElement = createModal({size: 'lg'})
      document.body.appendChild(modalElement)

      const modal = modalElement.querySelector('.usa-modal')
      expect(modal?.classList.contains('usa-modal--lg')).toBe(true)

      // Clean up and test invalid size
      modalElement.remove()
      document.body.innerHTML = ''

      const modalElement2 = createModal({
        modalId: 'test-modal-2',
        size: 'invalid-size',
      })
      document.body.appendChild(modalElement2)

      const modal2 = modalElement2.querySelector('.usa-modal')
      // Should not have any size class for invalid size
      expect(modal2?.classList.contains('usa-modal--invalid-size')).toBe(false)
      expect(modal2?.classList.contains('usa-modal--lg')).toBe(false)
      // Only base class should exist
      expect(modal2?.className.trim()).toBe('usa-modal')

      modalElement2.remove()
    })

    it('should handle XSS attempts in all fields', () => {
      modalElement = createModal({
        headingText: '<svg onload=alert("heading")>',
        descriptionHtml: '<iframe src="javascript:alert(\'desc\')"></iframe>',
        buttons: [{text: 'Click', action: 'malicious'}],
      })

      // Manually set malicious button HTML
      const buttonSlot = modalElement.querySelector('[slot="buttons"]')
      const button = buttonSlot?.querySelector('button')
      if (button) {
        button.innerHTML = '<a href="javascript:alert(\'btn\')">Click</a>'
      }

      document.body.appendChild(modalElement)

      const html = modalElement.innerHTML

      // Should not contain any javascript: protocols
      expect(html).not.toContain('javascript:')
      // Should not contain onload handlers
      expect(html).not.toContain('onload')
      // Should not contain iframe (DOMPurify removes it)
      expect(html).not.toContain('<iframe')
    })
  })

  describe('accessibility attributes', () => {
    it('should have correct aria-labelledby attribute', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      const modal = modalElement.querySelector('.usa-modal')
      expect(modal?.getAttribute('aria-labelledby')).toBe(
        'test-modal-modal-heading',
      )
    })

    it('should have correct aria-describedby attribute', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      const modal = modalElement.querySelector('.usa-modal')
      expect(modal?.getAttribute('aria-describedby')).toBe(
        'test-modal-modal-description',
      )
    })

    it('should have correct heading id', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      const heading = modalElement.querySelector('.usa-modal__heading')
      expect(heading?.id).toBe('test-modal-modal-heading')
    })

    it('should have correct description id', () => {
      modalElement = createModal()
      document.body.appendChild(modalElement)

      const descElement = modalElement.querySelector(
        '#test-modal-modal-description',
      )
      expect(descElement).toBeTruthy()
    })
  })
})
