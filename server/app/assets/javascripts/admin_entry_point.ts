/**
 * Entry point for admin bundle. Admin bundle is served on all pages that are
 * accessed by CiviForm and Program admins.
 */

import * as main from './main'
import * as accordion from './accordion'
import * as radio from './radio'
import * as toast from './toast'
import * as adminApplicationView from './admin_application_view'
import * as adminApplications from './admin_applications'
import * as adminPredicates from './admin_predicate_configuration'
import * as adminPrograms from './admin_programs'
import * as adminValidation from './admin_validation'
import * as devIcons from './dev_icons'
import * as modal from './modal'
import * as questionBank from './questionBank'
import * as preview from './preview'
import * as enumerator from './enumerator'

window.addEventListener('load', () => {
  main.init()
  accordion.init()
  radio.init()
  toast.init()
  adminApplicationView.init()
  adminApplications.init()
  adminPredicates.init()
  adminPrograms.init()
  adminValidation.init()
  devIcons.init()
  modal.init()
  questionBank.init()
  preview.init()
  enumerator.init()
})
