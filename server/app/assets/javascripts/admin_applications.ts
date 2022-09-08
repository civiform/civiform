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

    const selectedAppUrl =
      new URL(window.location.toString()).searchParams.get(
        'selectedApplication',
      ) || ''
    if (selectedAppUrl) {
      this.displayApplication(selectedAppUrl)
    }
  }

  private static extractSelectedApplicationUrl(): URL | null {
    const currentUrl = new URL(window.location.toString())
    const selectedAppValue =
      currentUrl.searchParams.get('selectedApplication') || ''
    if (!selectedAppValue) {
      return null
    }
    const selectedAppUrl = new URL(selectedAppValue)
    if (selectedAppUrl.origin !== window.location.origin) {
      return null
    }
    return selectedAppUrl
  }

  registerApplicationViewPostMessageListener() {
    window.addEventListener('message', (ev) => {
      if (ev.origin !== window.location.origin) {
        return
      }
      const message = ev.data as ApplicationViewMessage
      switch (message.messageType) {
        case 'UPDATE_STATUS': {
          this.updateStatus({
            programId: message.programId,
            applicationId: message.applicationId,
            data: message.data as UpdateStatusData,
          })
          break
        }
        case 'EDIT_NOTE': {
          this.editNote({
            programId: message.programId,
            applicationId: message.applicationId,
            data: message.data as EditNoteData,
          })
          break
        }
        default:
          throw new Error(`unrecognized message type ${message.messageType}`)
      }
    })
  }

  updateStatus({
    programId,
    applicationId,
    data,
  }: {
    programId: number
    applicationId: number
    data: UpdateStatusData
  }) {
    // Retrieve the CSRF token from the page.
    const csrfToken = this._assertNotNull(
      document.querySelector('input[name=csrfToken]'),
      'csrf token',
    )

    const newStatusEl = document.createElement('input')
    newStatusEl.name = 'newStatus'
    newStatusEl.value = data.newStatus
    const sendEmailEl = document.createElement('input')
    sendEmailEl.name = 'sendEmail'
    sendEmailEl.value = data.sendEmail
    const successRedirectUriEl = document.createElement('input')
    successRedirectUriEl.name = 'successRedirectUri'
    successRedirectUriEl.value = `${window.location.pathname}${window.location.search}`

    const formEl = document.createElement('form')
    formEl.method = 'POST'
    formEl.action = `/admin/programs/${programId}/applications/${applicationId}/updateStatus`
    formEl.appendChild(csrfToken)
    formEl.appendChild(successRedirectUriEl)
    formEl.appendChild(newStatusEl)
    formEl.appendChild(sendEmailEl)

    document.body.appendChild(formEl)
    formEl.submit()
  }

  editNote({
    programId,
    applicationId,
    data,
  }: {
    programId: number
    applicationId: number
    data: EditNoteData
  }) {
    // Retrieve the CSRF token from the page.
    const csrfToken = this._assertNotNull(
      document.querySelector('input[name=csrfToken]'),
      'csrf token',
    )
    const successRedirectUriEl = document.createElement('input')
    successRedirectUriEl.name = 'successRedirectUri'
    successRedirectUriEl.value = `${window.location.pathname}${window.location.search}`

    const noteEl = document.createElement('input')
    noteEl.name = 'note'
    noteEl.value = data.note

    const formEl = document.createElement('form')
    formEl.method = 'POST'
    formEl.action = `/admin/programs/${programId}/applications/${applicationId}/updateNote`
    formEl.appendChild(csrfToken)
    formEl.appendChild(noteEl)
    formEl.appendChild(successRedirectUriEl)

    document.body.appendChild(formEl)
    formEl.submit()
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

    const url = new URL(window.location.toString())
    url.searchParams.set('selectedApplication', applicationUrlPath)
    window.history.pushState({}, '', url.toString())

    this.displayApplication(applicationUrlPath)
  }

  displayApplication(applicationUrlPath: string) {
    const applicationUrl = new URL(applicationUrlPath, window.location.origin)
    if (applicationUrl.origin !== window.location.origin) {
      throw new Error(`Invalid application origin: ${applicationUrl.origin}`)
    }
    // Set iframe to display selected application.
    this.displayFrame.setAttribute('src', applicationUrl.toString())
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
  newStatus: string
  sendEmail: string
}

interface EditNoteData {
  note: string
}

window.addEventListener('load', () => new AdminApplications())
