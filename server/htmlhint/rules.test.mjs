// @vitest-environment node

import {readdirSync, readFileSync} from 'node:fs'
import {createRequire} from 'node:module'
import {fileURLToPath} from 'node:url'
import {dirname, join} from 'node:path'
import {beforeAll, describe, expect, it} from 'vitest'

// Load htmlhint and the rules with require(), the way `htmlhint --rulesdir`
// does, so the tests exercise the same module shape the CLI expects.
const require = createRequire(import.meta.url)
const {HTMLHint} = require('htmlhint')

const here = dirname(fileURLToPath(import.meta.url))
const rulesDir = join(here, 'rules')

beforeAll(() => {
  for (const file of readdirSync(rulesDir).filter((f) => f.endsWith('.js'))) {
    require(join(rulesDir, file))(HTMLHint)
  }
})

/** Rule ids reported when linting `html` with only `ruleId` enabled. */
function lint(ruleId, html) {
  return HTMLHint.verify(html, {[ruleId]: true}).map((m) => m.rule.id)
}

describe('.htmlhintrc', () => {
  // The CLI falls back to its default rules if the config doesn't parse, and
  // silently skips rule ids it doesn't know, so check both here.
  const raw = readFileSync(join(here, '..', '.htmlhintrc'), 'utf-8')
  const config = JSON.parse(raw.replace(/^\s*\/\/.*$/gm, ''))

  it('only enables rules that exist', () => {
    const unknown = Object.keys(config).filter((id) => !HTMLHint.rules[id])

    expect(unknown).toEqual([])
  })

  it('enables every custom rule', () => {
    const custom = Object.keys(HTMLHint.rules).filter((id) =>
      id.startsWith('th-'),
    )

    expect(custom.length).toBeGreaterThan(0)

    for (const id of custom) {
      expect(config[id], id).toBe(true)
    }
  })
})

describe('th-alt-require', () => {
  const rule = 'th-alt-require'

  it.each([
    ['static alt', '<img src="a.png" alt="A cat" />'],
    ['empty alt for decorative images', '<img src="a.png" alt="" />'],
    ['th:alt', '<img th:src="${src}" th:alt="${alt}" />'],
    ['data-th-alt', '<img data-th-src="${src}" data-th-alt="${alt}" />'],
    ['th:attr', '<img th:src="${src}" th:attr="alt=${alt}" />'],
    [
      'th:attr with several assignments',
      '<img th:attr="src=${src}, alt=${name + \' Logo\'}" />',
    ],
    ['th:replace', '<img th:replace="~{fragments :: logo}" />'],
    ['image input with alt', '<input type="image" src="go.png" alt="Go" />'],
    ['other tags', '<div th:text="${x}"></div>'],
  ])('allows %s', (_, html) => {
    expect(lint(rule, html)).toEqual([])
  })

  it.each([
    ['img with no alt', '<img th:src="${src}" />'],
    ['th:attr that does not set alt', '<img th:attr="title=${t}" />'],
    ['image input with no alt', '<input type="image" src="go.png" />'],
  ])('flags %s', (_, html) => {
    expect(lint(rule, html)).toEqual([rule])
  })
})

describe('th-inline-style-disabled', () => {
  const rule = 'th-inline-style-disabled'

  it.each([
    ['classes', '<div class="display-inline"></div>'],
    ['th:classappend', '<div th:classappend="${x} ? \'bold\'"></div>'],
    ['th:replace', '<div th:replace="~{f :: x}" style="color: red"></div>'],
    ['th:attr without style', '<div th:attr="data-x=${x}"></div>'],
  ])('allows %s', (_, html) => {
    expect(lint(rule, html)).toEqual([])
  })

  it.each([
    ['style', '<form style="display: inline"></form>'],
    ['th:style', '<div th:style="${css}"></div>'],
    ['th:styleappend', '<div th:styleappend="${css}"></div>'],
    ['data-th-style', '<div data-th-style="${css}"></div>'],
    ['th:attr', '<div th:attr="style=${css}"></div>'],
  ])('flags %s', (_, html) => {
    expect(lint(rule, html)).toEqual([rule])
  })
})

describe('th-inline-script-disabled', () => {
  const rule = 'th-inline-script-disabled'

  it.each([
    ['normal href', '<a href="/programs">Programs</a>'],
    [
      'th:href link expression',
      '<a th:href="@{/programs/{id}(id=${id})}">P</a>',
    ],
    [
      'placeholder javascript: href overridden by th:href',
      '<a href="javascript:void(0);" th:href="${loginLink}">Log in</a>',
    ],
    ['th:replace', '<a th:replace="~{f :: x}" onclick="go()"></a>'],
  ])('allows %s', (_, html) => {
    expect(lint(rule, html)).toEqual([])
  })

  it.each([
    ['onchange', '<select onchange="this.form.submit()"></select>'],
    ['onclick', '<button onclick="go()"></button>'],
    ['events outside the built-in list', '<details ontoggle="x()"></details>'],
    ['th:onclick', '<button th:onclick="${js}"></button>'],
    ['data-th-onclick', '<button data-th-onclick="${js}"></button>'],
    ['th:attr', '<button th:attr="onclick=${js}"></button>'],
    ['javascript: href', '<a href="javascript:void(0)">x</a>'],
    ['javascript: in th:href', '<a th:href="\'javascript:go()\'">x</a>'],
    ['javascript: form action', '<form action="javascript:go()"></form>'],
  ])('flags %s', (_, html) => {
    expect(lint(rule, html)).toEqual([rule])
  })
})
