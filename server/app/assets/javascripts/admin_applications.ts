class AdminApplications {
  private static BACKGROUND_GRAY_CLASS = 'bg-gray-200'
  private static CARD_SELECTOR = '.cf-admin-application-card'
  private static DISPLAY_FRAME_SELECTOR =
    'iframe[name="application-display-frame"]'

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
    this.cards = Array.from(
      document.querySelectorAll(AdminApplications.CARD_SELECTOR),
    )

    this.registerApplicationCardEventListeners()
    this.registerApplicationViewPostMessageListener()
  }

  registerApplicationViewPostMessageListener() {
    window.addEventListener('message', (ev) => {
      if (ev.origin !== window.location.origin) {
        return
      }
      alert(`got message from iframe! data: ${JSON.stringify(ev.data)}`)
      const message = ev.data as ApplicationViewMessage
      alert(
        `parsed: ${message.messageType} program id: ${this._assertIsNumber(
          message.programId,
        )} app id: ${this._assertIsNumber(message.applicationId)}`,
      )
      switch (message.messageType) {
        case 'UPDATE_STATUS': {
          const updateStatusData = message.data as UpdateStatusData
          alert(`${updateStatusData.status}`)
          break
        }
        case 'EDIT_NOTE': {
          alert('Edit note')
          break
        }
        default:
          throw new Error(`unrecognized message type ${message.messageType}`)
      }
    })
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

  _assertNotNull<T>(value: T | null | undefined, description: string): T {
    if (value == null) {
      throw new Error(`Expected ${description} not to be null.`)
    }

    return value
  }

  _assertIsNumber(value: any): number {
    if (typeof value !== 'number') {
      throw new Error(`Expected ${value} to be a number, got: ${typeof value}`)
    }
    return value
  }
}

interface ApplicationViewMessage {
  messageType: string
  programId: number
  applicationId: number
  data: UpdateStatusData | EditNoteData
}

interface UpdateStatusData {
  status: string
  sendEmail: string
}

interface EditNoteData {
  note: string
}

window.addEventListener('load', () => new AdminApplications())
