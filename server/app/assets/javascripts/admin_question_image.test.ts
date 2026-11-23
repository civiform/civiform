import {beforeEach, describe, expect, it, Mock, vi} from 'vitest'
import {init} from '@/admin_question_image'

vi.mock('@uswds/uswds/js/usa-file-input', () => ({
  default: {enable: vi.fn(), disable: vi.fn()},
}))

const TOO_LARGE_MESSAGE =
  'Your file is too large. Please choose a file less than 1 MB.'

function getInput(): HTMLInputElement {
  return document.getElementById('question-image-input') as HTMLInputElement
}

/** Selects a file of `sizeBytes` in the question image input and fires `change`. */
function selectFile(sizeBytes: number) {
  const input = getInput()
  const file = new File([new Uint8Array(sizeBytes)], 'image.png', {
    type: 'image/png',
  })
  Object.defineProperty(input, 'files', {value: [file], configurable: true})
  Object.defineProperty(input, 'value', {
    value: 'C:\\fakepath\\image.png',
    configurable: true,
    writable: true,
  })
  input.dispatchEvent(new Event('change', {bubbles: true}))
}

describe('question image upload size check', () => {
  // Stands in for the USWDS and htmx change listeners on the input.
  let inputChangeListener: Mock<(event: Event) => void>

  beforeEach(() => {
    // A fresh body drops the listeners init() added to the previous one.
    document.documentElement.replaceChild(
      document.createElement('body'),
      document.body,
    )
    document.body.innerHTML = `
      <form>
        <div class="usa-form-group">
          <input id="questionImageDescription" value="Alt text" />
          <span id="error-message-questionImageDescription"></span>
        </div>
        <input
          id="question-image-input"
          type="file"
          data-file-limit-mb="1"
          data-file-too-large-message="${TOO_LARGE_MESSAGE}"
        />
        <div
          id="question-image-file-input-errors"
          class="text-red-600 text-xs p-1 hidden"
        ></div>
      </form>`
    init()
    inputChangeListener = vi.fn<(event: Event) => void>()
    getInput().addEventListener('change', inputChangeListener)
  })

  it('stops a file over the limit and shows the error', () => {
    selectFile(1024 * 1024 + 1)

    expect(inputChangeListener).not.toHaveBeenCalled()
    const errors = document.getElementById('question-image-file-input-errors')
    expect(errors?.textContent).toBe(TOO_LARGE_MESSAGE)
    expect(errors?.classList.contains('hidden')).toBe(false)
    expect(getInput().value).toBe('')
    expect(getInput().getAttribute('aria-invalid')).toBe('true')
  })

  it('lets a file at the limit through', () => {
    selectFile(1024 * 1024)

    expect(inputChangeListener).toHaveBeenCalledOnce()
    const errors = document.getElementById('question-image-file-input-errors')
    expect(errors?.classList.contains('hidden')).toBe(true)
  })

  it('clears the invalid state when the next file is within the limit', () => {
    selectFile(1024 * 1024 + 1)
    expect(getInput().getAttribute('aria-invalid')).toBe('true')

    selectFile(1024)

    expect(inputChangeListener).toHaveBeenCalledOnce()
    expect(getInput().hasAttribute('aria-invalid')).toBe(false)
  })
})
