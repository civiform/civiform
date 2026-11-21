// Helpers for writing HTMLHint rules that understand Thymeleaf attributes.
//
// HTMLHint parses templates as plain HTML, so it can't tell that
// `th:alt="${x}"` produces an `alt` attribute, or that an element carrying
// `th:replace` is thrown away at render time. These helpers answer those
// questions from the attribute list of a single tag.
//
// This file lives outside htmlhint/rules/ on purpose: htmlhint --rulesdir
// loads every .js file in that directory as a rule.

'use strict'

// th:attr, th:attrappend and th:attrprepend set arbitrary attributes from a
// comma-separated list of `name=expression` pairs.
const ATTR_SETTERS = ['th:attr', 'th:attrappend', 'th:attrprepend']

// Attributes whose presence means the host element is not rendered: Thymeleaf
// swaps it for the referenced fragment, so whatever is on it doesn't matter.
const REPLACING_ATTRS = ['th:replace', 'data-th-replace']

/**
 * Returns the Thymeleaf attribute names that produce the HTML attribute
 * `name`: `th:name` and its data-attribute form `data-th-name`.
 */
function thymeleafForms(name) {
  return [`th:${name}`, `data-th-${name}`]
}

/** Lowercase attribute name -> value for a tag event. */
function attrMap(event) {
  const map = new Map()

  for (const attr of event.attrs) {
    map.set(attr.name.toLowerCase(), attr.value)
  }

  return map
}

/**
 * Names of the attributes a th:attr-style setter assigns. Good enough for
 * linting: it can over-match when an expression itself contains `, name=`,
 * which only matters to rules that flag an attribute's presence.
 */
function namesSetBy(value) {
  const names = []
  const re = /(?:^|,)\s*([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=/g
  let match

  while ((match = re.exec(value)) !== null) {
    names.push(match[1].toLowerCase())
  }

  return names
}

/** Every attribute name th:attr, th:attrappend, or th:attrprepend sets. */
function namesSetByAttrSetters(attrs) {
  const names = []

  for (const setter of ATTR_SETTERS) {
    for (const form of [setter, `data-${setter.replace(':', '-')}`]) {
      if (attrs.has(form)) {
        names.push(...namesSetBy(attrs.get(form)))
      }
    }
  }

  return names
}

/**
 * Whether the rendered element can carry `name`, either written directly,
 * through th:name / data-th-name, or through th:attr.
 */
function hasRenderedAttr(attrs, name) {
  if (attrs.has(name)) {
    return true
  }

  if (thymeleafForms(name).some((form) => attrs.has(form))) {
    return true
  }

  return namesSetByAttrSetters(attrs).includes(name)
}

/** Whether Thymeleaf replaces this element with a fragment. */
function isReplaced(attrs) {
  return REPLACING_ATTRS.some((name) => attrs.has(name))
}

/**
 * The attribute value that ends up in the rendered HTML for `name`.
 *
 * A th:name attribute overrides the static one, so a static value next to it is
 * only a natural-template placeholder and never reaches the browser. Returns
 * `{source, value}` naming the attribute the value came from, or null if
 * neither form is present.
 */
function renderedValue(attrs, name) {
  for (const form of thymeleafForms(name)) {
    if (attrs.has(form)) {
      return {source: form, value: attrs.get(form)}
    }
  }

  if (attrs.has(name)) {
    return {source: name, value: attrs.get(name)}
  }

  return null
}

/** Column of the attribute named `name` in the tag event, for reporting. */
function attrColumn(event, name) {
  const col = event.col + event.tagName.length + 1
  const attr = event.attrs.find((a) => a.name.toLowerCase() === name)

  return attr ? col + attr.index : col
}

/** The raw source of the attribute named `name`, for reporting. */
function attrRaw(event, name) {
  const attr = event.attrs.find((a) => a.name.toLowerCase() === name)

  return attr ? attr.raw : event.raw
}

module.exports = {
  attrColumn,
  attrMap,
  attrRaw,
  hasRenderedAttr,
  isReplaced,
  namesSetByAttrSetters,
  renderedValue,
  thymeleafForms,
}
