import {default as uswdsFileInput} from '@uswds/uswds/js/usa-file-input'

const FILE_INPUT_ID = 'question-image-input'
const ALT_INPUT_ID = 'question-image-description'

export function init() {
  const fileInput = document.getElementById(
    FILE_INPUT_ID,
  ) as HTMLInputElement | null
  const altInput = document.getElementById(
    ALT_INPUT_ID,
  ) as HTMLInputElement | null

  if (!fileInput || !altInput) {
    return
  }

  const syncFileInputState = () => {
    const hasAltText = altInput.value.trim().length > 0
    if (hasAltText) {
      uswdsFileInput.enable(fileInput)
    } else {
      uswdsFileInput.disable(fileInput)
    }
  }

  // Enable/disable dynamically as the admin types or clears alt-text
  altInput.addEventListener('input', syncFileInputState)
  altInput.addEventListener('change', syncFileInputState)

  // Initialize state on page load
  syncFileInputState()
}
