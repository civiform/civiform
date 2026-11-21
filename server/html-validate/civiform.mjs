// html-validate plugin with the lint rules written for CiviForm templates.
// A rule only runs once it is turned on in .htmlvalidate.mjs.
import {definePlugin} from 'html-validate'
import HtmxAttributes from './rules/htmx-attributes.mjs'
import NoInlineScript from './rules/no-inline-script.mjs'

export default definePlugin({
  name: 'civiform',
  rules: {
    'civiform/htmx-attributes': HtmxAttributes,
    'civiform/no-inline-script': NoInlineScript,
  },
})
