import {attachRedirectToPageListeners} from './main'

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

// htmx does not provide Typescript definitions for its custom events, so we have to define them ourselves
// For more info about fields on an htmx request see https://htmx.org/events/#htmx:configRequest
interface HtmxRequest {
  formData: FormData
}

export function init() {
  new AdminImportView()
}
