class AdminApplications {
  private static BACKGROUND_GRAY_CLASS = 'bg-gray-200'
  private static BACKGROUND_WHITE_CLASS = 'bg-white'
  private static CARD_SELECTOR = '.cf-admin-application-card'
  static DISPLAY_FRAME_SELECTOR = 'iframe[name="application-display-frame"]'

  // This value should be kept in sync with that in AdminApplicationController.java.
  private static SELECTED_APPLICATION_URI_PARAM_NAME = 'selectedApplicationUri'

  // This value should be kept in sync with ProgramApplicationListView.java
  private static SHOW_DOWNLOAD_MODAL = 'showDownloadModal'

  // These values should be kept in sync with those in AdminApplicationController.java.
  private static REDIRECT_URI_INPUT_NAME = 'redirectUri'
  // These values should be kept in sync with those in admin_application_view.ts
  // and ProgramApplicationView.java.
  private static CURRENT_STATUS_INPUT_NAME = 'currentStatus'
  private static NEW_STATUS_INPUT_NAME = 'newStatus'
  private static SEND_EMAIL_INPUT_NAME = 'sendEmail'
  private static NOTE_INPUT_NAME = 'note'

  private cards: Array<HTMLElement>

  constructor(private readonly displayFrame: Element) {
    this.cards = Array.from(
      document.querySelectorAll(AdminApplications.CARD_SELECTOR),
    )

    this.registerApplicationCardEventListeners()
    this.registerApplicationViewPostMessageListener()
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
    this.cards.forEach((card) => {
      const child = card.children[0]
      this._assertNotNull(child, 'card inner div').classList.remove(
        AdminApplications.BACKGROUND_GRAY_CLASS,
      )
      this._assertNotNull(child, 'card inner div').classList.add(
        AdminApplications.BACKGROUND_WHITE_CLASS,
      )
    })

    // Add selection style to selected card.
    const child = selectedCard.children[0]
    this._assertNotNull(child, 'card inner div').classList.add(
      AdminApplications.BACKGROUND_GRAY_CLASS,
    )

    this._assertNotNull(child, 'card inner div').classList.remove(
      AdminApplications.BACKGROUND_WHITE_CLASS,
    )

    // Preserve the selected application in the URL so that any attempts to refresh the page
    // maintain the selected application.
    const url = new URL(window.location.toString())
    url.searchParams.set(
      AdminApplications.SELECTED_APPLICATION_URI_PARAM_NAME,
      applicationUrlPath,
    )

    url.searchParams.set(AdminApplications.SHOW_DOWNLOAD_MODAL, 'false')
    window.history.pushState({}, '', url.toString())

    // Set iframe to display selected application.
    this.displayFrame.setAttribute('src', applicationUrlPath)
  }

  _assertNotNull<T>(value: T | null | undefined, description: string): T {
    if (value == null) {
      throw new Error(`Expected ${description} not to be null.`)
    }

    return value
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

  private updateStatus({
    programId,
    applicationId,
    data,
  }: {
    programId: number
    applicationId: number
    data: UpdateStatusData
  }) {
    this.submitFormWithInputs({
      action: `/admin/programs/${programId}/applications/${applicationId}/updateStatus`,
      inputs: [
        {
          inputName: AdminApplications.REDIRECT_URI_INPUT_NAME,
          inputValue: this.currentRelativeUrl(),
        },
        {
          inputName: AdminApplications.CURRENT_STATUS_INPUT_NAME,
          inputValue: data.currentStatus,
        },
        {
          inputName: AdminApplications.NEW_STATUS_INPUT_NAME,
          inputValue: data.newStatus,
        },
        {
          inputName: AdminApplications.SEND_EMAIL_INPUT_NAME,
          inputValue: data.sendEmail,
        },
      ],
    })
  }

  private editNote({
    programId,
    applicationId,
    data,
  }: {
    programId: number
    applicationId: number
    data: EditNoteData
  }) {
    this.submitFormWithInputs({
      action: `/admin/programs/${programId}/applications/${applicationId}/updateNote`,
      inputs: [
        {
          inputName: AdminApplications.REDIRECT_URI_INPUT_NAME,
          inputValue: this.currentRelativeUrl(),
        },
        {inputName: AdminApplications.NOTE_INPUT_NAME, inputValue: data.note},
      ],
    })
  }

  private submitFormWithInputs({
    action,
    inputs,
  }: {
    action: string
    inputs: {inputName: string; inputValue: string}[]
  }) {
    const formEl = document.createElement('form')
    formEl.hidden = true
    formEl.method = 'POST'
    formEl.action = action
    // Retrieve the CSRF token from the page.
    formEl.appendChild(
      this._assertNotNull(
        document.querySelector('input[name=csrfToken]'),
        'csrf token',
      ),
    )
    inputs.forEach(({inputName, inputValue}) => {
      // For multiline text, a "textarea" is required since "input" elements are single-line:
      //  https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/text
      const elementType = inputValue.includes('\n') ? 'textarea' : 'input'
      const inputEl = document.createElement(elementType)
      inputEl.hidden = true
      inputEl.name = inputName
      inputEl.value = inputValue
      formEl.appendChild(inputEl)
    })

    document.body.appendChild(formEl)
    formEl.submit()
  }

  private currentRelativeUrl(): string {
    return `${window.location.pathname}${window.location.search}`
  }
}

interface ApplicationViewMessage {
  messageType: string
  programId: number
  applicationId: number
  data: UpdateStatusData | EditNoteData
}

interface UpdateStatusData {
  currentStatus: string
  newStatus: string
  sendEmail: string
}

interface EditNoteData {
  note: string
}

export function init() {
  const frame = document.querySelector(AdminApplications.DISPLAY_FRAME_SELECTOR)
  if (frame) {
    new AdminApplications(frame)
  }
}
