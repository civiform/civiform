import {Rule} from 'html-validate'

/** Whether an attribute is `expected`, or dynamic and so might be. */
function mayBe(attr, expected) {
  return Boolean(attr) && (attr.isDynamic || expected.includes(attr.value))
}

function isSubmitControl(node) {
  const type = node.getAttribute('type')

  switch (node.tagName) {
    case 'button':
      return !type || mayBe(type, ['submit'])
    case 'input':
      return mayBe(type, ['submit', 'image'])
    // <cf:button> renders a <button> and defaults to type="button".
    case 'cf:button':
      return mayBe(type, ['submit'])
    default:
      return false
  }
}

/** Content that is only known at render time and might hold the control. */
function isRenderedLater(node) {
  return node.hasAttribute('th:replace') || node.hasAttribute('th:insert')
}

/**
 * WCAG H32: a form needs a submit control. This replaces the built-in
 * wcag/h32 rule, which finds submit buttons with a selector and so can't see
 * `<cf:button type="submit">`: html-validate selectors don't match a tag name
 * with a colon in it.
 *
 * A form passes when a fragment is included inside it, because the fragment
 * might hold the button.
 */
export default class FormSubmit extends Rule {
  documentation() {
    return {
      description:
        'A `<form>` needs a submit button: `<cf:button type="submit">`, `<button>` or `<input type="submit">`.',
      url: 'https://www.w3.org/WAI/WCAG22/Techniques/html/H32',
    }
  }

  setup() {
    this.on('dom:ready', ({document}) => {
      const controls = document.querySelectorAll('*').filter(isSubmitControl)

      for (const form of document.querySelectorAll('form')) {
        const inside = form.querySelectorAll('*')
        const id = form.getAttribute('id')

        const hasSubmit =
          isRenderedLater(form) ||
          inside.some(
            (node) => isRenderedLater(node) || isSubmitControl(node),
          ) ||
          // <button form="form-id"> outside of the form.
          controls.some((node) => {
            const owner = node.getAttribute('form')

            return owner && id && (owner.isDynamic || owner.value === id.value)
          })

        if (!hasSubmit) {
          this.report({
            node: form,
            message: '<form> must have a submit button',
          })
        }
      }
    })
  }
}
