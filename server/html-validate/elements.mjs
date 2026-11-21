// Element metadata for the tags html-validate doesn't know: Thymeleaf's
// `<th:block>` and the CiviForm `cf:*` tags (see app/views/tags).
import {defineMetadata} from 'html-validate'
import html5 from 'html-validate/elements/html5'

// `<th:block>` never reaches the browser, only its children do. Allow it
// wherever any element can go, and check its children against its parent.
const container = {
  flow: true,
  phrasing: true,
  metadata: true,
  scriptSupporting: true,
  transparent: true,
}

// The cf:* tags render a form control together with its USWDS wrapper, label
// and error message.
const component = {flow: true, phrasing: true}

// <cf:button> renders a <button>.
const button = {...component, interactive: true}

export default defineMetadata({
  'th:block': container,
  'th:div': container,

  'cf:button': button,
  'cf:checkbox': component,
  'cf:icon': component,
  'cf:input': component,
  'cf:radio': component,
  'cf:select': component,
  'cf:textarea': component,
  // Options of a <cf:checkbox> or <cf:radio> group.
  item: {},

  // Elements that are only valid under a specific parent are often wrapped in
  // a <th:block>, or sit at the root of a fragment that is one. html-validate
  // selectors can't match a tag name with a colon in it, so <li> uses the
  // script-supporting category <th:block> is in, and <dt>/<dd> allow any
  // element between them and the <dl>. Anything else that sits directly under
  // a <dl> is still reported by element-permitted-content.
  li: {permittedParent: [...html5.li.permittedParent, '@script']},
  dt: {
    requiredAncestors: [
      ...html5.dt.requiredAncestors,
      'dl > * > dt',
      'dl > * > div > dt',
    ],
  },
  dd: {
    requiredAncestors: [
      ...html5.dd.requiredAncestors,
      'dl > * > dd',
      'dl > * > div > dd',
    ],
  },
})
