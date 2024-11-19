import {attachRedirectToPageListeners} from './main'
import {HtmxRequest} from './htmx_request'

class AdminImportView {
  constructor() {
    this.addRedirectListeners()
    this.addStringifyJsonListener()
  }

  addRedirectListeners() {
    document.addEventListener('htmx:afterSwap', () => {
      attachRedirectToPageListeners()
    })
  }

  addStringifyJsonListener() {
    document.body.addEventListener('htmx:configRequest', function (evt) {
      const customEvent = evt as CustomEvent<HtmxRequest>
      const formData = customEvent.detail.formData
      const programJson = formData.get('programJson') as string
      const trimmedProgramJson = JSON.stringify(JSON.parse(programJson))
      customEvent.detail.formData.set('programJson', trimmedProgramJson)
    })
  }
}

export function init() {
  new AdminImportView()
}
