// Lints the Thymeleaf templates in app/views. Run with `npm run lint:html`.
//
// html-validate/thymeleaf.mjs maps th:* attributes onto the HTML they render,
// html-validate/elements.mjs describes <th:block> and the cf:* tags, and
// html-validate/civiform.mjs has the rules written for this project.
import civiform from './html-validate/civiform.mjs'
import elements from './html-validate/elements.mjs'
import thymeleaf from './html-validate/thymeleaf.mjs'

export default {
  root: true,
  plugins: [thymeleaf, civiform],
  // The prettier preset turns off the rules that would fight the formatter
  // (see formatter/fmt).
  extends: ['html-validate:recommended', 'html-validate:prettier'],
  elements: ['html5', elements],
  transform: {'\\.html$': 'thymeleaf'},
  rules: {
    // Every tag has to be html, <th:block>, or a cf:* tag the dialect knows.
    'no-unknown-elements': 'error',

    // htmx ignores an attribute or swap style it doesn't know.
    'civiform/htmx-attributes': 'error',

    // conf/application.conf puts a nonce in script-src, so the browser never
    // runs an inline event handler or a javascript: URL.
    'civiform/no-inline-script': 'error',

    // Style only. `required="required"` and `hidden=""` behave the same as
    // the bare attribute, and browsers add the <tbody> themselves.
    'attribute-boolean-style': 'off',
    'attribute-empty-style': 'off',
    'prefer-tbody': 'off',

    // toast.ts reads camelCase attributes such as `canDismiss`.
    'attr-case': ['error', {style: ['lowercase', 'camelcase']}],

    // Thymeleaf expressions use `<` and `&&` inside attribute values.
    'no-raw-characters': 'off',

    // The title is set at render time with th:text, so its length in the
    // template is the length of the expression.
    'long-title': 'off',

    // USWDS markup puts aria-label and aria-labelledby on elements that get
    // their role from uswds.js at runtime (modals, step indicators), and
    // repeats implicit roles such as `<img role="img">`.
    'aria-label-misuse': 'off',
    'no-redundant-role': 'off',

    // Question fieldsets are named with aria-labelledby instead of a <legend>.
    'wcag/h71': 'off',

    // Forms usually submit through <cf:button type="submit">. html-validate
    // looks up submit buttons with a selector, and its selectors can't match
    // a tag name that has a colon in it.
    'wcag/h32': 'off',

    // Off until the existing templates are cleaned up. Each fix needs a look
    // at the page: an explicit type can change which CSS applies, and several
    // buttons that default to submit were probably meant to be type="button".
    'no-implicit-button-type': 'off',
    'no-implicit-input-type': 'off',
    'prefer-button': 'off',
    'prefer-native-element': 'off',
  },
}
