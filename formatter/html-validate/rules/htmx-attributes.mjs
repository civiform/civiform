import {Rule} from 'html-validate'

// The attributes htmx 2 reads. See https://htmx.org/reference/#attributes
const ATTRIBUTES = new Set([
  'hx-boost',
  'hx-confirm',
  'hx-delete',
  'hx-disable',
  'hx-disabled-elt',
  'hx-disinherit',
  'hx-encoding',
  'hx-ext',
  'hx-get',
  'hx-headers',
  'hx-history',
  'hx-history-elt',
  'hx-include',
  'hx-indicator',
  'hx-inherit',
  'hx-params',
  'hx-patch',
  'hx-post',
  'hx-preserve',
  'hx-prompt',
  'hx-push-url',
  'hx-put',
  'hx-replace-url',
  'hx-request',
  'hx-select',
  'hx-select-oob',
  'hx-swap',
  'hx-swap-oob',
  'hx-sync',
  'hx-target',
  'hx-trigger',
  'hx-validate',
  'hx-vals',
  'hx-vars',
])

// `hx-on:click` and `hx-on-click`, and the attributes of the response-targets
// extension: `hx-target-error`, `hx-target-404`, `hx-target-4xx`, `hx-target-5*`.
const ATTRIBUTE_PATTERNS = [/^hx-on[:-]./, /^hx-target-(error|[\dx*]{1,3})$/]

const SWAP_STYLES = [
  'innerHTML',
  'outerHTML',
  'textContent',
  'beforebegin',
  'afterbegin',
  'beforeend',
  'afterend',
  'delete',
  'none',
]

const SWAP_MODIFIER =
  /^(swap|settle|transition|ignoreTitle|scroll|show|focus-scroll):/

function isKnownAttribute(name) {
  return (
    ATTRIBUTES.has(name) ||
    ATTRIBUTE_PATTERNS.some((pattern) => pattern.test(name))
  )
}

/**
 * Returns the part of an hx-swap value htmx would not understand, or null.
 * The value is an optional swap style followed by modifiers, such as
 * `innerHTML show:top`.
 */
function invalidSwapToken(value) {
  const tokens = value.trim().split(/\s+/).filter(Boolean)

  return (
    tokens.find((token, index) => {
      const isStyle = index === 0 && SWAP_STYLES.includes(token)

      return !isStyle && !SWAP_MODIFIER.test(token)
    }) ?? null
  )
}

/**
 * htmx ignores an attribute it doesn't know, so a misspelled `hx-` attribute
 * or swap style fails without an error anywhere. The thymeleaf transformer
 * yields `th:hx-post` and `th:attr="hx-post=..."` as `hx-post` with a dynamic
 * value, so those names are checked too and their values are left alone.
 */
export default class HtmxAttributes extends Rule {
  documentation() {
    return {
      description:
        'An `hx-` attribute has to be one htmx reads, and a static `hx-swap` value has to be a swap style htmx knows.',
      url: 'https://htmx.org/reference/#attributes',
    }
  }

  setup() {
    this.on('attr', (event) => {
      // htmx also reads every attribute with a `data-` prefix.
      const name = event.key.toLowerCase().replace(/^data-/, '')

      if (!name.startsWith('hx-')) {
        return
      }

      const written = event.originalAttribute ?? event.key

      if (!isKnownAttribute(name)) {
        this.report({
          node: event.target,
          message: `Unknown htmx attribute "${written}"`,
          location: event.keyLocation,
        })
        return
      }

      if (name === 'hx-swap' && typeof event.value === 'string') {
        const token = invalidSwapToken(event.value)

        if (token !== null) {
          this.report({
            node: event.target,
            message: `"${token}" is not a valid hx-swap value. Use one of ${SWAP_STYLES.join(', ')}, then modifiers such as show:top`,
            location: event.valueLocation,
          })
        }
      }
    })
  }
}
