import {default as uswdsFileInput} from '@uswds/uswds/js/usa-file-input'
import {hideError, showError} from '@/file_upload_util'

const FILE_INPUT_ID = 'question-image-input'
const ALT_INPUT_IDS = ['questionImageDescription', 'question-image-description']
const ALT_ERROR_IDS = [
  'error-message-questionImageDescription',
  'error-message-question-image-description',
]
const DELETE_BUTTON_ID = 'delete-question-image-button'
const EXISTING_ALERT_ID = 'existing-image-alert'

export function init() {
  const fileInput = document.getElementById(
    FILE_INPUT_ID,
  ) as HTMLInputElement | null

  let altInput: HTMLInputElement | null = null
  for (const id of ALT_INPUT_IDS) {
    const el = document.getElementById(id)
    if (el instanceof HTMLInputElement) {
      altInput = el
      break
    }
  }

  if (!fileInput || !altInput) return

  let errorSpan: HTMLElement | null = null
  for (const id of ALT_ERROR_IDS) {
    const el = document.getElementById(id)
    if (el) {
      errorSpan = el
      break
    }
  }

  const deleteButton = document.getElementById(DELETE_BUTTON_ID)
  const existingAlert = document.getElementById(EXISTING_ALERT_ID)
  const formGroup = altInput.closest('.usa-form-group')
  const form = altInput.closest('form')

  // Combines dropzone lock/unlock, required state, and visual USWDS error toggle
  const syncAndValidate = (): boolean => {
    const hasAltText = altInput.value.trim().length > 0
    const hasImage =
      fileInput.getAttribute('data-has-existing-image') === 'true' ||
      fileInput.value !== ''

    // 1. File dropzone is enabled only when alt-text is present
    if (hasAltText) {
      uswdsFileInput.enable(fileInput)
    } else {
      uswdsFileInput.disable(fileInput)
    }

    // 2. Alt-text is required if an image is present
    altInput.required = hasImage
    const isValid = !hasImage || hasAltText

    if (isValid) {
      hideError(errorSpan, altInput)
      formGroup?.classList.remove('usa-form-group--error')
    } else {
      showError(errorSpan, altInput)
      formGroup?.classList.add('usa-form-group--error')
    }

    return isValid
  }

  // Resets the UI when an image is deleted
  const resetImageState = () => {
    altInput.value = ''
    fileInput.value = ''
    fileInput.setAttribute('data-has-existing-image', 'false')

    deleteButton?.classList.add('hidden')
    existingAlert?.classList.add('hidden')

    // Clean up previews and restore USWDS instructions
    const dropzone =
      fileInput.closest('.usa-file-input') ||
      fileInput.closest('.usa-form-group')
    if (dropzone) {
      dropzone
        .querySelectorAll(
          '.usa-file-input__preview, .usa-file-input__preview-heading, .usa-file-input__accepted-files-message',
        )
        .forEach((el) => el.remove())

      const instructions = dropzone.querySelector(
        '.usa-file-input__instructions',
      )
      if (instructions) {
        instructions.removeAttribute('hidden')
      }

      const target = dropzone.querySelector('.usa-file-input__target')
      if (target) {
        target.classList.remove('has-invalid-file')
      }
    }
    fileInput.setAttribute('aria-label', 'Drag file here or choose from folder')

    syncAndValidate()
  }

  // Event Listeners
  altInput.addEventListener('input', syncAndValidate)

  form?.addEventListener('submit', (e) => {
    if (!syncAndValidate()) {
      e.preventDefault()
      altInput.focus()
    }
  })

  document.body.addEventListener('htmx:afterRequest', (event) => {
    const target = event.detail.elt
    const status = event.detail.xhr.status

    if (target.id === DELETE_BUTTON_ID && status === 200) {
      resetImageState()
    } else if (target.id === FILE_INPUT_ID && status === 200) {
      fileInput.setAttribute('data-has-existing-image', 'true')
      deleteButton?.classList.remove('hidden')
      existingAlert?.classList.remove('hidden')
      syncAndValidate()
    }
  })

  // Initial state check
  syncAndValidate()
}
