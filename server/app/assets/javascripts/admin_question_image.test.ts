import {beforeEach, describe, expect, it, vi} from 'vitest'
import {init} from '@/admin_question_image'

vi.mock('@uswds/uswds/js/usa-file-input', () => ({
  default: {enable: vi.fn(), disable: vi.fn()},
}))

const TOO_LARGE_MESSAGE =
  'Your file is too large. Please choose a file less than 1 MB.'

/** Selects a file of `sizeBytes` in the question image input. */
function selectFile(sizeBytes: number) {
  const input = document.getElementById(
    'question-image-input',
  ) as HTMLInputElement
  const file = new File([new Uint8Array(sizeBytes)], 'image.png', {
    type: 'image/png',
  })
  Object.defineProperty(input, 'files', {value: [file], configurable: true})
  Object.defineProperty(input, 'value', {
    value: 'C:\\fakepath\\image.png',
    configurable: true,
  })
}

/** Dispatches the htmx:beforeRequest event for the upload and returns it. */
function dispatchBeforeRequest(): CustomEvent {
  const event = new CustomEvent('htmx:beforeRequest', {
    cancelable: true,
    detail: {elt: document.getElementById('question-image-input')},
  })
  document.body.dispatchEvent(event)
  return event
}

describe('question image upload size check', () => {
  beforeEach(() => {
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
  })

  it('blocks the upload and shows the error when the file is over the limit', () => {
    selectFile(1024 * 1024 + 1)

    const event = dispatchBeforeRequest()

    expect(event.defaultPrevented).toBe(true)
    const errors = document.getElementById('question-image-file-input-errors')
    expect(errors?.textContent).toBe(TOO_LARGE_MESSAGE)
    expect(errors?.classList.contains('hidden')).toBe(false)
  })

  it('lets the upload through when the file is at the limit', () => {
    selectFile(1024 * 1024)

    const event = dispatchBeforeRequest()

    expect(event.defaultPrevented).toBe(false)
    const errors = document.getElementById('question-image-file-input-errors')
    expect(errors?.classList.contains('hidden')).toBe(true)
  })
})
