// Thymeleaf-aware replacement for HTMLHint's inline-script-disabled.
//
// Our CSP sets a script nonce, and browsers ignore 'unsafe-inline' once a
// nonce is present, so inline event handlers and javascript: URLs are blocked
// at runtime. They fail silently in the browser; this rule fails them at lint
// time instead.
//
// Differences from the built-in rule:
//   - Catches every on* handler, not a fixed list of event names, plus
//     th:on*, data-th-on*, and handlers set through th:attr.
//   - Checks the value that renders. `href="javascript:void(0)"` next to a
//     th:href is a natural-template placeholder that Thymeleaf overwrites,
//     so it isn't flagged; a javascript: URL inside the th:href is.

'use strict'

const {
  attrColumn,
  attrMap,
  attrRaw,
  isReplaced,
  namesSetByAttrSetters,
  renderedValue,
} = require('../thymeleaf')

const EVENT_HANDLER = /^(?:data-th-|th:)?on[a-z]+$/
const URL_ATTRS = ['href', 'src', 'action', 'formaction', 'xlink:href']
const JAVASCRIPT_URL = /^\W*javascript:/i

module.exports = function (HTMLHint) {
  HTMLHint.addRule({
    id: 'th-inline-script-disabled',
    description:
      'Inline event handlers (on*, th:on*) and javascript: URLs are not allowed; CSP blocks them.',
    init(parser, reporter) {
      parser.addListener('tagstart', (event) => {
        const attrs = attrMap(event)

        if (isReplaced(attrs)) {
          return
        }

        for (const name of attrs.keys()) {
          if (EVENT_HANDLER.test(name)) {
            reporter.error(
              `Inline event handler [ ${name} ] is blocked by CSP. Attach a listener from TypeScript instead.`,
              event.line,
              attrColumn(event, name),
              this,
              attrRaw(event, name),
            )
          }
        }

        if (namesSetByAttrSetters(attrs).some((n) => EVENT_HANDLER.test(n))) {
          reporter.error(
            'Inline event handler set through th:attr is blocked by CSP. Attach a listener from TypeScript instead.',
            event.line,
            event.col + event.tagName.length + 1,
            this,
            event.raw,
          )
        }

        for (const name of URL_ATTRS) {
          const rendered = renderedValue(attrs, name)

          if (rendered && JAVASCRIPT_URL.test(rendered.value)) {
            reporter.error(
              `javascript: URL in [ ${rendered.source} ] is blocked by CSP. Use a <button> with a TypeScript listener instead.`,
              event.line,
              attrColumn(event, rendered.source),
              this,
              attrRaw(event, rendered.source),
            )
          }
        }
      })
    },
  })
}
