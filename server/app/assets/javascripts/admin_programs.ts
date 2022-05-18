class AdminPrograms {
  private static PROGRAM_ADIN_LIST_SELECTOR = '.cf-admin-program-card-list'
  private static PROGRAM_CARDS_SELECTOR = '.cf-admin-program-card'
  private static PROGRAM_CARDS_PLACEHOLDER_SELECTOR =
    '.cf-admin-program-card-list-placeholder'
  private static NAME_ATTRIBUTE = 'data-name'
  private static LAST_UPDATED_MILLIS = 'data-last-updated-millis'

  constructor() {
    const cardsParent = document.querySelector(
      AdminPrograms.PROGRAM_ADIN_LIST_SELECTOR
    )
    if (!cardsParent) {
      return
    }
    const cardsPlaceholder = cardsParent.querySelector(
      AdminPrograms.PROGRAM_CARDS_PLACEHOLDER_SELECTOR
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

  sortCards(cardsParent: HTMLElement) {
    const cards = Array.from(
      cardsParent.querySelectorAll(
        AdminPrograms.PROGRAM_CARDS_SELECTOR
      ) as any as Array<HTMLElement>
    )
    cards.sort((first, second) => {
      const firstComparator = this.comparatorObject(first)
      const secondComparator = this.comparatorObject(second)

      let lastUpdatedComparator =
        secondComparator.lastUpdatedMillis - firstComparator.lastUpdatedMillis
      // TODO(issue #): Consider elements within 1 second of each
      // other as equivalent.
      if (Math.abs(lastUpdatedComparator) < 1000) {
        lastUpdatedComparator = 0
      }

      return (
        lastUpdatedComparator ||
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

  comparatorObject(el: HTMLElement) {
    const lastUpdatedMillisString =
      el.getAttribute(AdminPrograms.LAST_UPDATED_MILLIS) || ''
    const lastUpdatedMillis = Number(lastUpdatedMillisString)
    return {
      name: el.getAttribute(AdminPrograms.NAME_ATTRIBUTE) || '',
      lastUpdatedMillis,
    }
  }
}

window.addEventListener('load', () => new AdminPrograms())
