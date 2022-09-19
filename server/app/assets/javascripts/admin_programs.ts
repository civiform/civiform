class AdminPrograms {
  private static PROGRAM_CARDS_SELECTOR = '.cf-admin-program-card'
  private static PROGRAM_LINK_ATTRIBUTE = 'data-copyable-program-link'

  static attachCopyProgramLinkListeners() {
    const withCopyableProgramLink = Array.from(
      document.querySelectorAll(
        `${AdminPrograms.PROGRAM_CARDS_SELECTOR} [${AdminPrograms.PROGRAM_LINK_ATTRIBUTE}]`,
      ),
    )
    withCopyableProgramLink.forEach((el) => {
      const programLink = el.getAttribute(AdminPrograms.PROGRAM_LINK_ATTRIBUTE)
      if (!programLink) {
        console.warn(
          `Empty ${AdminPrograms.PROGRAM_LINK_ATTRIBUTE} for element`,
        )
        return
      }
      el.addEventListener('click', () => {
        AdminPrograms.copyProgramLinkToClipboard(programLink)
      })
    })
  }

  /**
   * Attempts to copy the given content to the clipboard.
   * @param {string} content
   * @return {Promise<boolean>} indicating whether the content was copied to the clipboard
   */
  static async tryCopyToClipboard(content: string): Promise<boolean> {
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

  static async copyProgramLinkToClipboard(programLink: string) {
    const succeeded = await AdminPrograms.tryCopyToClipboard(programLink)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const toastController = window['toastController'] as any
    if (succeeded) {
      toastController.showToastMessage({
        id: `program-link-${Math.random()}`,
        content: 'Program link copied to clipboard',
        duration: 3000,
        type: 'success',
        canDismiss: true,
        canIgnore: false,
      })
    } else {
      toastController.showToastMessage({
        id: `program-link-${Math.random()}`,
        content: `Could not copy program link to clipboard: ${programLink}`,
        duration: -1,
        type: 'warning',
        canDismiss: true,
        canIgnore: false,
      })
    }
  }
}

window.addEventListener('load', () => {
  AdminPrograms.attachCopyProgramLinkListeners()
})
