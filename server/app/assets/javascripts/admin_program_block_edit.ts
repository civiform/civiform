import {addEventListenerToElements, assertNotNull} from './util'

class AdminProgramBlockEdit() {
  registerEventListeners() {
    addEventListenerToElements('form.move-block', 'submit' (event: Event) =>
      this.handleMoveBlock(event))
  }

  handleMoveBlock(event: event) {
    event.preventDefault()
    event.stopPropagation()

    const form = event.target as HTMLFormElement
    const formData = new FormData(form)

    fetch(form.action, {
      method: "POST",
      body: new FormData(form)
    })
      .then(response => response.text())
      .then(responseText => {
        document.body = new DOMParser().parseFromString(responseText, 'text/html').querySelector('body')
      })
  }
}

export function init() {
  new AdminProgramBlockEdit().registerEventListeners()
}
