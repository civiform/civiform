class AdminPrograms {
  private static PROGRAM_ADIN_LIST_SELECTOR = '.cf-program-admin-list'
  private static PROGRAM_CARDS_SELECTOR = '.cf-admin-program-card'
  private static NAME_ATTRIBUTE = 'data-name'
  private static LAST_UPDATED_MILLIS = 'data-last-updated-millis'

  constructor() {
    const cardsParent = document.querySelector(AdminPrograms.PROGRAM_ADIN_LIST_SELECTOR)
    if (!cardsParent) {
      return
    }
    this.sortCards(cardsParent as HTMLElement)
  }

  sortCards(cardsParent: HTMLElement) {
    const cards = Array.from(cardsParent.querySelectorAll(
      AdminPrograms.PROGRAM_CARDS_SELECTOR
    ) as any as Array<HTMLElement>)
    cards.sort((first, second) => {
      const firstComparator = this.comparatorObject(first)
      const secondComparator = this.comparatorObject(second)

      return secondComparator.lastUpdatedMillis - firstComparator.lastUpdatedMillis ||
        firstComparator.name.localeCompare(secondComparator.name)
    })
    cards.forEach((el) => {
      cardsParent.appendChild(el)
    })
  }

  comparatorObject(el: HTMLElement) {
    const lastUpdatedMillisString = el.getAttribute(AdminPrograms.LAST_UPDATED_MILLIS) || '';
    const lastUpdatedMillis = Number(lastUpdatedMillisString);
    return {
      name: el.getAttribute(AdminPrograms.NAME_ATTRIBUTE) || '',
      lastUpdatedMillis,
    }
  }
}

window.addEventListener('load', () => new AdminPrograms())
