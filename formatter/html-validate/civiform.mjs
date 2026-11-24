// html-validate plugin with the lint rules written for CiviForm templates.
// A rule only runs once it is turned on in formatter/.htmlvalidate.mjs.
import {definePlugin} from 'html-validate'
import FormSubmit from './rules/form-submit.mjs'
import HtmxAttributes from './rules/htmx-attributes.mjs'
import NoInlineScript from './rules/no-inline-script.mjs'

export default definePlugin({
  name: 'civiform',
  rules: {
    'civiform/form-submit': FormSubmit,
    'civiform/htmx-attributes': HtmxAttributes,
    'civiform/no-inline-script': NoInlineScript,
  },
})
