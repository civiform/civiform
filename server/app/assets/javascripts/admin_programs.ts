import {showToastMessage} from './toast'

const PROGRAM_CARDS_SELECTOR = '.cf-admin-program-card'
const PROGRAM_LINK_ATTRIBUTE = 'data-copyable-program-link'

function attachCopyProgramLinkListeners() {
  const withCopyableProgramLink = Array.from(
    document.querySelectorAll(
      `${PROGRAM_CARDS_SELECTOR} [${PROGRAM_LINK_ATTRIBUTE}]`,
    ),
  )
  withCopyableProgramLink.forEach((el) => {
    const programLink = el.getAttribute(PROGRAM_LINK_ATTRIBUTE)
    if (!programLink) {
      console.warn(`Empty ${PROGRAM_LINK_ATTRIBUTE} for element`)
      return
    }
    el.addEventListener('click', () => {
      void copyProgramLinkToClipboard(programLink)
    })
  })
}

/**
 * Attempts to copy the given content to the clipboard.
 * @param {string} content
 * @return {Promise<boolean>} indicating whether the content was copied to the clipboard
 */
async function tryCopyToClipboard(content: string): Promise<boolean> {
  if (!window.navigator['clipboard']) {
    return false
  }
  try {
    await window.navigator['clipboard'].writeText(content)
    return true
  } catch {
    return false
  }
}

<<<<<<< HEAD
async function copyProgramLinkToClipboard(programLink: string) {
  const succeeded = await tryCopyToClipboard(programLink)
  if (succeeded) {
    showToastMessage({
      id: `program-link-${Math.random()}`,
      content: 'Program link copied to clipboard',
      duration: 3000,
      type: 'success',
      canDismiss: true,
      canIgnore: false,
    })
  } else {
    showToastMessage({
      id: `program-link-${Math.random()}`,
      content: `Could not copy program link to clipboard: ${programLink}`,
      duration: -1,
      type: 'warning',
      canDismiss: true,
      canIgnore: false,
    })
  }
}

export function init() {
  attachCopyProgramLinkListeners()
=======
export function init() {
  AdminPrograms.attachCopyProgramLinkListeners()
>>>>>>> main
}
