// import {assertNotNull} from './util'
import {attachRedirectToPageListeners} from './main'

class AdminImportView {
  constructor() {
    this.addRedirectListeners()
  }

  addRedirectListeners() {
    document.addEventListener('htmx:afterSwap', () => {
      attachRedirectToPageListeners()
    })
  }
}

export function init() {
  new AdminImportView()
}
