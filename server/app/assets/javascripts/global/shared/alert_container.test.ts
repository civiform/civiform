import {describe, it, expect, beforeEach, vi} from 'vitest'
import {AlertContainer} from '@/global/shared/alert_container'

function createContainer(): HTMLElement {
  const container = document.createElement('div')
  container.id = 'alertContainer'
  document.body.appendChild(container)
  return container
}

describe('AlertContainer', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  describe('show', () => {
    it('renders a slim alert with the type variant and text', () => {
      const container = createContainer()

      expect(AlertContainer.show('success', 'Saved successfully')).toBe(true)

      const alert = container.querySelector('.usa-alert')!
      expect(alert.classList.contains('usa-alert--slim')).toBe(true)
      expect(alert.classList.contains('usa-alert--success')).toBe(true)
      expect(alert.querySelector('.usa-alert__text')!.textContent).toBe(
        'Saved successfully',
      )
    })

    it('replaces existing content instead of stacking', () => {
      const container = createContainer()

      AlertContainer.show('error', 'First')
      AlertContainer.show('info', 'Second')

      expect(container.querySelectorAll('.usa-alert').length).toBe(1)
      expect(container.querySelector('.usa-alert__text')!.textContent).toBe(
        'Second',
      )
    })

    it('returns false when the page has no alert container', () => {
      expect(AlertContainer.show('error', 'No home for me')).toBe(false)
    })
  })

  describe('clear', () => {
    it('empties the container', () => {
      const container = createContainer()
      AlertContainer.show('warning', 'Something')

      AlertContainer.clear()

      expect(container.children.length).toBe(0)
    })

    it('does nothing when the page has no alert container', () => {
      expect(() => AlertContainer.clear()).not.toThrow()
    })
  })

  describe('showErrorSummary', () => {
    it('renders the heading and one entry per item', () => {
      const container = createContainer()

      AlertContainer.showErrorSummary('Fix these', [
        {message: 'Name is required', controlId: 'name'},
        {message: 'Email is invalid'},
      ])

      const alert = container.querySelector('.usa-alert')!
      expect(alert.classList.contains('usa-alert--error')).toBe(true)
      expect(alert.querySelector('.usa-alert__heading')!.textContent).toBe(
        'Fix these',
      )
      const entries = alert.querySelectorAll('li')
      expect(entries.length).toBe(2)
    })

    it('moves focus to the summary', () => {
      const container = createContainer()

      AlertContainer.showErrorSummary('Fix these', [
        {message: 'Name is required', controlId: 'name'},
      ])

      expect(document.activeElement).toBe(container.querySelector('.usa-alert'))
    })

    it('links entries with a controlId and focuses the control on click', () => {
      createContainer()
      const input = document.createElement('input')
      input.id = 'name'
      document.body.appendChild(input)

      AlertContainer.showErrorSummary('Fix these', [
        {message: 'Name is required', controlId: 'name'},
      ])

      const link = document.querySelector<HTMLAnchorElement>(
        '#alertContainer li a',
      )!
      expect(link.getAttribute('href')).toBe('#name')

      link.dispatchEvent(new MouseEvent('click', {cancelable: true}))
      expect(document.activeElement).toBe(input)
    })

    it('renders entries without a controlId as plain text', () => {
      const container = createContainer()

      AlertContainer.showErrorSummary('Fix these', [
        {message: 'Something page-level went wrong'},
      ])

      const entry = container.querySelector('li')!
      expect(entry.querySelector('a')).toBeNull()
      expect(entry.textContent).toBe('Something page-level went wrong')
    })

    it('returns false when the page has no alert container', () => {
      expect(AlertContainer.showErrorSummary('Fix these', [])).toBe(false)
    })
  })

  describe('init', () => {
    it('scrolls newly inserted alerts into view', async () => {
      const container = createContainer()
      const scrollSpy = vi.fn()
      HTMLElement.prototype.scrollIntoView = scrollSpy

      AlertContainer.init()
      AlertContainer.show('error', 'Inserted later')

      // Wait for the MutationObserver to fire (macrotask boundary).
      await new Promise((r) => setTimeout(r, 0))

      expect(scrollSpy).toHaveBeenCalled()
      expect(container.querySelector('.usa-alert')).not.toBeNull()
    })

    it('does nothing when the page has no alert container', () => {
      expect(() => AlertContainer.init()).not.toThrow()
    })
  })
})
