// Element metadata for the tags html-validate doesn't know: Thymeleaf's
// `<th:block>` and the CiviForm `cf:*` tags.
//
// The cf:* entries mirror what the dialect in app/views/tags checks when it
// renders a tag (CiviFormProcessorDialect and the classes in
// app/views/tags/settings), so a mistake is reported here instead of as an
// exception on the page. A `th:` attribute counts as its plain form, and its
// value isn't checked because it is only known at render time.
//
// The XSD files in schemas/ describe the same tags for editor completion.
// Where they disagree with the Java, this file follows the Java, because
// that is what runs.
import {readdirSync} from 'node:fs'
import {join} from 'node:path'
import {defineMetadata, metadataHelper} from 'html-validate'
import html5 from 'html-validate/elements/html5'

const {allowedIfAttributeIsPresent} = metadataHelper

// `<th:block>` never reaches the browser, only its children do. Allow it
// wherever any element can go, and check its children against its parent.
const container = {
  flow: true,
  phrasing: true,
  metadata: true,
  scriptSupporting: true,
  transparent: true,
}

// FormSettings.isValidName. The built-in valid-id rule already covers what
// FormSettings.isValidId checks.
const VALID_NAME = /^[a-zA-Z0-9_.-]+(?:\[[a-zA-Z0-9_.-]*\])*$/
const NOT_BLANK = /\S/

// AbstractElementModelProcessor reads these with Boolean.parseBoolean, so
// only "true" turns one on. A bare `required` or `required="required"` is
// read as false and the attribute is left off the rendered control.
const flag = {enum: ['true', 'false']}

// FormSettings.validate: every form control needs an id, a name and a label,
// and validation-class and validation-field only work as a pair.
const formControlAttributes = {
  id: {required: true},
  name: {required: true, enum: [VALID_NAME]},
  label: {required: true, enum: [NOT_BLANK]},
  size: {enum: ['2xs', 'xs', 'sm', 'small', 'md', 'medium', 'lg', 'xl', '2xl']},
  'validation-class': {
    allowed: allowedIfAttributeIsPresent('validation-field'),
  },
  'validation-field': {
    allowed: allowedIfAttributeIsPresent('validation-class'),
  },
  required: flag,
  readonly: flag,
  disabled: flag,
  'is-valid': flag,
}

/** A cf:* tag that renders a form control with its label and error message. */
function formControl(attributes = {}, extra = {}) {
  return {
    flow: true,
    phrasing: true,
    attributes: {...formControlAttributes, ...attributes},
    ...extra,
  }
}

// CheckboxElementTagModelProcessor and RadioElementTagModelProcessor skip
// every child that isn't an <item>.
const optionGroup = {permittedContent: ['item']}

// IconElementTagProcessor looks the icon up by file name and throws outside
// of prod when the file isn't there.
const ICONS = readdirSync(
  join(import.meta.dirname, '../app/assets/Images/uswds'),
)
  .filter((file) => file.endsWith('.svg'))
  .map((file) => file.slice(0, -'.svg'.length))

export default defineMetadata({
  'th:block': container,
  'th:div': container,

  // InputFormSettings.ALLOWED_TYPES
  'cf:input': formControl({
    type: {
      enum: [
        'color',
        'date',
        'datetime-local',
        'email',
        'hidden',
        'month',
        'number',
        'password',
        'range',
        'search',
        'tel',
        'text',
        'time',
        'url',
        'week',
      ],
    },
  }),
  'cf:textarea': formControl({'markdown-enabled': flag}),
  'cf:select': formControl(),
  // CheckboxFormSettings.ALLOWED_COLUMNS
  'cf:checkbox': formControl(
    {columns: {enum: ['2', '3', '4']}, tiled: flag},
    optionGroup,
  ),
  'cf:radio': formControl({tiled: flag}, optionGroup),

  // ItemFormSettings.validate. An <item> is in no content category, so it is
  // reported anywhere an element only takes flow or phrasing content, which
  // leaves the two option groups above as the places it can go.
  item: {
    attributes: {
      id: {required: true, enum: [NOT_BLANK]},
      label: {required: true, enum: [NOT_BLANK]},
      value: {required: true, enum: [NOT_BLANK]},
      selected: flag,
      disabled: flag,
    },
  },

  // ButtonFormSettings. A <cf:button> renders a <button>.
  'cf:button': {
    flow: true,
    phrasing: true,
    interactive: true,
    attributes: {
      type: {enum: ['button', 'reset', 'submit']},
      size: {enum: ['big']},
      disabled: flag,
      inverse: flag,
      variant: {
        enum: [
          'secondary',
          'outline',
          'accent-cool',
          'accent-warm',
          'base',
          'unstyled',
        ],
      },
    },
  },

  'cf:icon': {
    flow: true,
    phrasing: true,
    attributes: {type: {required: true, enum: ICONS}},
  },

  // Elements that are only valid under a specific parent are often wrapped in
  // a <th:block>, or sit at the root of a fragment that is one. html-validate
  // selectors can't match a tag name with a colon in it, so <li> uses the
  // script-supporting category <th:block> is in, and <dt>/<dd> allow any
  // element between them and the <dl>. Anything else that sits directly under
  // a <dl> is still reported by element-permitted-content.
  li: {permittedParent: [...html5.li.permittedParent, '@script']},
  dt: {
    requiredAncestors: [
      ...html5.dt.requiredAncestors,
      'dl > * > dt',
      'dl > * > div > dt',
    ],
  },
  dd: {
    requiredAncestors: [
      ...html5.dd.requiredAncestors,
      'dl > * > dd',
      'dl > * > div > dd',
    ],
  },
})
