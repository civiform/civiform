import {Rule} from 'html-validate'

// `onclick`, `onchange` and every other event handler attribute.
const EVENT_HANDLER = /^on[a-z]+$/
// htmx runs `hx-on:click` through `new Function`, which the policy also blocks.
const HTMX_EVENT_HANDLER = /^(data-)?hx-on[:-]/
const URL_ATTRIBUTES = ['href', 'src', 'action', 'formaction', 'xlink:href']
const JAVASCRIPT_URL = /^\W*javascript:/i

/** The text of a static value or of the expression behind a th: attribute. */
function valueText(attr) {
  return attr.isDynamic ? attr.value.expr : (attr.value ?? '')
}

/**
 * conf/application.conf puts a nonce in script-src. Browsers ignore
 * 'unsafe-inline' once a nonce is present, so an inline event handler or a
 * `javascript:` URL never runs and fails without an error on the page.
 *
 * The thymeleaf transformer yields `th:onclick` and `th:attr="onclick=..."`
 * as `onclick` with a dynamic value, so those are reported too.
 */
export default class NoInlineScript extends Rule {
  documentation() {
    return {
      description:
        'The Content Security Policy blocks inline event handlers and `javascript:` URLs. Attach a listener from TypeScript instead.',
    }
  }

  setup() {
    this.on('element:ready', ({target}) => {
      // th:replace swaps the element for a fragment, so its attributes are
      // never rendered.
      if (target.hasAttribute('th:replace')) {
        return
      }

      for (const attr of target.attributes) {
        const name = attr.key.toLowerCase()

        if (EVENT_HANDLER.test(name) || HTMX_EVENT_HANDLER.test(name)) {
          this.report({
            node: target,
            message: `Inline event handler "${attr.originalAttribute ?? attr.key}" is blocked by the Content Security Policy. Attach a listener from TypeScript instead`,
            location: attr.keyLocation,
          })
        }
      }

      for (const name of URL_ATTRIBUTES) {
        const candidates = target.getAttribute(name, true)

        // A th: attribute overrides the static one, which is then only a
        // placeholder that never reaches the browser.
        const rendered =
          candidates.find((attr) => attr.isDynamic) ?? candidates[0]

        if (rendered && JAVASCRIPT_URL.test(valueText(rendered))) {
          this.report({
            node: target,
            message: `javascript: URL in "${rendered.originalAttribute ?? rendered.key}" is blocked by the Content Security Policy. Use a <button> with a TypeScript listener instead`,
            location: rendered.keyLocation,
          })
        }
      }
    })
  }
}
