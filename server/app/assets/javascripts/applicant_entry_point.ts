/**
 * Entry point for applicant bundle. Applicant bundle is served on all pages
 * that are used by applicants and trusted intermediaries.
 */

import * as main from './main'
import * as enumerator from './enumerator'
import * as radio from './radio'
import * as toast from './toast'
import * as modal from './modal'
import * as fileUpload from './file_upload'
import * as azureDelete from './azure_delete'
import * as azureUpload from './azure_upload'
import * as phoneNumber from './phone'
import * as apiDocs from './api_docs'
import htmx from 'htmx.org'

declare global {
  interface Window {
    // eslint-disable-next-line  @typescript-eslint/no-explicit-any
    htmx: any
  }
}

window.htmx = htmx

window.addEventListener('load', () => {
  main.init()
  enumerator.init()
  radio.init()
  toast.init()
  modal.init()
  fileUpload.init()
  azureDelete.init()
  azureUpload.init()
  phoneNumber.init()
  // API docs are publicly visible, so we need the supporting scripts here.
  apiDocs.init()
})
