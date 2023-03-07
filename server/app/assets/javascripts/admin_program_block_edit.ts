import {addEventListenerToElements, assertNotNull} from './util'

class AdminProgramBlockEdit {
  public registerEventListeners() {
    addEventListenerToElements('form.move-block', 'submit', (event: Event) =>
      this.handleMoveBlock(event),
    )
  }

  private handleMoveBlock(event: Event) {
    event.preventDefault()

    const form = event.target as HTMLFormElement

    fetch(form.action, {
      method: 'POST',
      body: new FormData(form),
    })
      .then((response) => response.text())
      .then((responseText) => {
        document.body = new DOMParser()
          .parseFromString(responseText, 'text/html')
          .querySelector('body') as HTMLElement

        this.registerEventListeners()
      })
  }
}

export function init() {
  new AdminProgramBlockEdit().registerEventListeners()
}
