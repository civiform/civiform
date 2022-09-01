class AdminPrograms {
  private static PROGRAM_ADIN_LIST_SELECTOR = '.cf-admin-program-card-list'
  private static PROGRAM_CARDS_SELECTOR = '.cf-admin-program-card'
  private static PROGRAM_CARDS_PLACEHOLDER_SELECTOR =
    '.cf-admin-program-card-list-placeholder'
  private static NAME_ATTRIBUTE = 'data-name'
  private static LAST_UPDATED_MILLIS = 'data-last-updated-millis'
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

  static sortCardsOnLoad() {
    const cardsParent = document.querySelector(
      AdminPrograms.PROGRAM_ADIN_LIST_SELECTOR,
    )
    if (!cardsParent) {
      return
    }
    const cardsPlaceholder = cardsParent.querySelector(
      AdminPrograms.PROGRAM_CARDS_PLACEHOLDER_SELECTOR,
    )
    try {
      this.sortCards(cardsParent as HTMLElement)
    } finally {
      // Make sure to always show the cards, even
      // if there was an error while sorting.
      cardsParent.classList.remove('invisible')
      if (cardsPlaceholder) {
        cardsPlaceholder.classList.add('hidden')
      }
    }
  }

  static sortCards(cardsParent: HTMLElement) {
    const cards = Array.from(
      cardsParent.querySelectorAll(AdminPrograms.PROGRAM_CARDS_SELECTOR),
    )
    cards.sort((first, second) => {
      const firstComparator = AdminPrograms.comparatorObject(first)
      const secondComparator = AdminPrograms.comparatorObject(second)

      return (
        secondComparator.lastUpdatedMillis -
          firstComparator.lastUpdatedMillis ||
        firstComparator.name.localeCompare(secondComparator.name)
      )
    })
    cards.forEach((el) => {
      // Note: Calling appendChild on this element will reuse
      // the existing element since it's already in the DOM.
      // This means that any attached event handlers will remain.
      cardsParent.appendChild(el)
    })
  }

  static comparatorObject(el: Element) {
    const lastUpdatedMillisString =
      el.getAttribute(AdminPrograms.LAST_UPDATED_MILLIS) || ''
    const lastUpdatedMillis = Number(lastUpdatedMillisString)
    return {
      name: el.getAttribute(AdminPrograms.NAME_ATTRIBUTE) || '',
      lastUpdatedMillis,
    }
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
  AdminPrograms.sortCardsOnLoad()
  AdminPrograms.attachCopyProgramLinkListeners()
})
