// Thymeleaf-aware replacement for HTMLHint's inline-style-disabled.
//
// Templates style through USWDS classes, not inline styles. The built-in rule
// only catches a literal `style` attribute; this one also catches th:style,
// th:styleappend, their data-th-* forms, and `style=` set through th:attr.

'use strict'

const {
  attrColumn,
  attrMap,
  attrRaw,
  isReplaced,
  namesSetByAttrSetters,
  thymeleafForms,
} = require('../thymeleaf')

const STYLE_ATTRS = [
  'style',
  ...thymeleafForms('style'),
  ...thymeleafForms('styleappend'),
]

module.exports = function (HTMLHint) {
  HTMLHint.addRule({
    id: 'th-inline-style-disabled',
    description:
      'Inline styles are not allowed, including th:style and th:attr="style=...". Use USWDS classes.',
    init(parser, reporter) {
      parser.addListener('tagstart', (event) => {
        const attrs = attrMap(event)

        if (isReplaced(attrs)) {
          return
        }

        for (const name of STYLE_ATTRS) {
          if (attrs.has(name)) {
            reporter.error(
              `Inline style [ ${name} ] is not allowed. Use USWDS utility classes instead.`,
              event.line,
              attrColumn(event, name),
              this,
              attrRaw(event, name),
            )
          }
        }

        if (namesSetByAttrSetters(attrs).includes('style')) {
          reporter.error(
            'Inline style set through th:attr is not allowed. Use USWDS utility classes instead.',
            event.line,
            event.col + event.tagName.length + 1,
            this,
            event.raw,
          )
        }
      })
    },
  })
}
