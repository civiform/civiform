// html-validate plugin that teaches the linter how Thymeleaf templates render.
//
// html-validate only sees the template source. Without this plugin it reports
// `<img th:alt="...">` as missing alt text and `<h1 th:text="...">` as an empty
// heading. The transformer below maps Thymeleaf attributes onto the HTML they
// produce so the standard rules can check the rendered result.
import {DynamicValue, definePlugin} from 'html-validate'

// Processors that control rendering. They don't set an attribute of the same
// name on the output element.
const LOGIC_PROCESSORS = new Set([
  'assert',
  'case',
  'each',
  'fragment',
  'if',
  'inline',
  'insert',
  'object',
  'remove',
  'replace',
  'switch',
  'text',
  'unless',
  'utext',
  'with',
])

// Processors that set more than one attribute.
const MULTI_ATTRIBUTE_PROCESSORS = {
  'alt-title': ['alt', 'title'],
  'lang-xmllang': ['lang', 'xml:lang'],
  classappend: ['class'],
  styleappend: ['style'],
}

// Matches the attribute names in `th:attr="lang=${a},dir=${b}"`.
const ATTR_ASSIGNMENT = /(?:^|,)\s*([\w:-]+)\s*=/g

// Processors that give the element a body at render time.
const CONTENT_PROCESSORS = ['th:text', 'th:utext', 'th:replace', 'th:insert']

/** Returns the HTML attributes a `th:*` attribute sets when rendered. */
function renderedAttributeNames(processor, value) {
  if (['attr', 'attrappend', 'attrprepend'].includes(processor)) {
    return Array.from(value.matchAll(ATTR_ASSIGNMENT), (match) => match[1])
  }

  if (processor in MULTI_ATTRIBUTE_PROCESSORS) {
    return MULTI_ATTRIBUTE_PROCESSORS[processor]
  }

  return LOGIC_PROCESSORS.has(processor) ? [] : [processor]
}

// Thymeleaf reads `data-th-text` the same as `th:text`.
const THYMELEAF_ATTRIBUTE = /^(?:th:|data-th-)(.+)$/

function* processAttribute(attr) {
  yield attr

  const match = THYMELEAF_ATTRIBUTE.exec(attr.key)

  if (!match || typeof attr.value !== 'string') {
    return
  }

  const processor = match[1]

  // Yield the data-th-* spelling as th:* as well, so the rest of this plugin
  // and the rules only have to look for one of them.
  if (!attr.key.startsWith('th:')) {
    yield {...attr, key: `th:${processor}`, originalAttribute: attr.key}
  }

  for (const key of renderedAttributeNames(processor, attr.value)) {
    yield {
      key,
      value: new DynamicValue(attr.value),
      quote: attr.quote,
      originalAttribute: attr.key,
    }
  }
}

function processElement(node) {
  const processor = CONTENT_PROCESSORS.find((name) => node.hasAttribute(name))

  if (processor) {
    const expression = String(node.getAttributeValue(processor))
    node.appendText({dynamic: expression}, node.location)
  }

  // th:replace swaps the whole element for a fragment, so the element's own
  // content and role say nothing about what is rendered. Its tag still stands
  // in for the fragment when checking where it is allowed to go.
  if (node.hasAttribute('th:replace') && node.meta) {
    node.loadMeta({
      ...node.meta,
      requiredContent: undefined,
      aria: {...node.meta.aria, implicitRole: () => null},
    })
  }
}

// `<!-- [html-validate-disable-next rule] -->` is an HTML comment, so
// Thymeleaf would send it to the browser. A parser-level comment
// (`<!--/* ... */-->`) is stripped at render time, but html-validate doesn't
// recognise a directive inside one. Blank out the `/*` and `*/` so it does.
// The replacement keeps the same length so reported locations stay correct.
const PARSER_LEVEL_DIRECTIVE =
  /<!--\/\*(\s*\[html-validate-[^\]]*\]\s*)\*\/-->/g

function exposeDirectives(data) {
  return data.replace(PARSER_LEVEL_DIRECTIVE, '<!--  $1  -->')
}

function thymeleaf(source) {
  return [
    {
      ...source,
      data: exposeDirectives(source.data),
      originalData: source.originalData ?? source.data,
      hooks: {processAttribute, processElement},
    },
  ]
}

// The version of the transformer API this function is written against.
thymeleaf.api = 1

export default definePlugin({
  name: 'thymeleaf',
  transformer: {default: thymeleaf},
})
