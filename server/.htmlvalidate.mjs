// Lints the Thymeleaf templates in app/views. Run with `npm run lint:html`.
//
// html-validate/thymeleaf.mjs maps th:* attributes onto the HTML they render,
// html-validate/elements.mjs describes <th:block> and the cf:* tags, and
// html-validate/civiform.mjs has the rules written for this project.
//
// To silence a finding, put a directive in a Thymeleaf parser-level comment so
// it isn't sent to the browser, and say why:
//
//   <!--/* [html-validate-disable-next no-dup-id -- only one branch renders] */-->
//
// The templates ported from j2html keep the legacy markup until their page is
// rewritten. They share directories with new templates, so each one opts out
// of the rules it breaks with a `html-validate-disable` comment on its first
// line. Every other template is checked against all of the rules below.
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

    // aria-label on any element that can take an accessible name is fine.
    'aria-label-misuse': ['error', {allowAnyNamable: true}],

    // A link that is styled as a button keeps role="button". The rule still
    // reports every other role that has a native element.
    'prefer-native-element': ['error', {exclude: ['button']}],

    // civiform/form-submit checks the same thing and also understands
    // <cf:button type="submit">.
    'wcag/h32': 'off',
    'civiform/form-submit': 'error',
  },
}
