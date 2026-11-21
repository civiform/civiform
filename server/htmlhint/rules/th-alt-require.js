// Thymeleaf-aware replacement for HTMLHint's alt-require.
//
// The built-in rule only sees a literal `alt` attribute, so it flags every
// `<img th:src="..." th:alt="...">` in our templates. This one accepts alt
// text supplied by th:alt, data-th-alt, or th:attr, and skips elements that
// th:replace discards.

'use strict'

const {attrMap, hasRenderedAttr, isReplaced} = require('../thymeleaf')

module.exports = function (HTMLHint) {
  HTMLHint.addRule({
    id: 'th-alt-require',
    description:
      '<img> and <input type="image"> must have alt text, set directly or with th:alt / th:attr.',
    init(parser, reporter) {
      parser.addListener('tagstart', (event) => {
        const tagName = event.tagName.toLowerCase()
        const attrs = attrMap(event)
        const isImageInput =
          tagName === 'input' && attrs.get('type')?.toLowerCase() === 'image'

        if (tagName !== 'img' && !isImageInput) {
          return
        }

        if (isReplaced(attrs) || hasRenderedAttr(attrs, 'alt')) {
          return
        }

        reporter.error(
          `<${tagName}> needs an alt attribute. Use alt="" for decorative images, or th:alt for dynamic text.`,
          event.line,
          event.col + tagName.length + 1,
          this,
          event.raw,
        )
      })
    },
  })
}
