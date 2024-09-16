/**
 * Entry point for applicant bundle. Applicant bundle is served on all pages
 * that are used by applicants and trusted intermediaries.
 */

import * as main from './main'
import * as languageSelector from './language_selector'
import * as enumerator from './enumerator'
import * as radio from './radio'
import * as toast from './toast'
import * as modal from './modal'
import * as northStarModal from './north_star_modal'
import * as fileUpload from './file_upload'
import * as azureDelete from './azure_delete'
import * as azureUpload from './azure_upload'
import * as phoneNumber from './phone'
import * as apiDocs from './api_docs'
import * as trustedIntermediary from './trusted_intermediary'
import htmx from './htmx'

// todo not sure why this doesn't init from the import above. maybe it's unused to it's getting cleaned up?
// @ts-expect-error Extra noise because of linting rules thinking this is wrong, but it is not.
window.htmx = htmx

window.addEventListener('load', () => {
  initializeEverything()
  htmx.on('htmx:afterSettle', () => {
    initializeEverything()
  })
})

function initializeEverything(): void {
  main.init()
  languageSelector.init()
  enumerator.init()
  radio.init()
  toast.init()
  modal.init()
  northStarModal.init()
  fileUpload.init()
  azureDelete.init()
  azureUpload.init()
  phoneNumber.init()
  // API docs are publicly visible, so we need the supporting scripts here.
  apiDocs.init()
  trustedIntermediary.init()
}
