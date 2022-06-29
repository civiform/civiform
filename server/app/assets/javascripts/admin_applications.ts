class AdminApplications {
  private static BACKGROUND_GRAY_CLASS = 'bg-gray-200'
  private static CARD_SELECTOR = '.cf-admin-application-card'
  private static DISPLAY_FRAME_SELECTOR = '#application-display-frame'

  private displayFrame: Element
  private cards: Array<HTMLElement>

  constructor() {
    const frame = document.querySelector(
      AdminApplications.DISPLAY_FRAME_SELECTOR,
    )

    if (frame == null) {
      throw new Error('Application display frame not found!')
    }

    this.displayFrame = frame
    this.cards = document.querySelectorAll(
      AdminApplications.CARD_SELECTOR,
    ) as any as Array<HTMLElement>

    this.registerApplicationCardEventListeners()
  }

  registerApplicationCardEventListeners() {
    this.cards.forEach((cardEl: HTMLElement) => {
      const linkEl = cardEl.querySelector('a')

      if (linkEl == null) {
        throw new Error('No `a` found for application card')
      }

      const href = linkEl.getAttribute('href')

      if (href == null) {
        throw new Error('Missing href for application card view link')
      }

      linkEl.addEventListener('click', (event) => {
        event.preventDefault()
        event.stopPropagation()

        this.viewApplication(cardEl, href)
      })
    })
  }

  viewApplication(selectedCard: Element, applicationUrlPath: string) {
    // Remove selection style from previously selected card.
    this.cards.forEach((card) =>
      this._assertNotNull(card.children[0], 'card inner div').classList.remove(
        AdminApplications.BACKGROUND_GRAY_CLASS,
      ),
    )

    // Add selection style to selected card.
    this._assertNotNull(
      selectedCard.children[0],
      'card inner div',
    ).classList.add(AdminApplications.BACKGROUND_GRAY_CLASS)

    // Set iframe to display selected application.
    this.displayFrame.setAttribute('src', applicationUrlPath)
  }

  _assertNotNull(value: any, description: string) {
    if (value == null) {
      throw new Error(`Expected ${description} not to be null.`)
    }

    return value
  }
}

window.addEventListener('load', () => new AdminApplications())
