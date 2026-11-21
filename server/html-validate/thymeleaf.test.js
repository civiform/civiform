// @vitest-environment node
import {describe, expect, it} from 'vitest'
import {lint} from './testing.js'

describe('thymeleaf attributes', () => {
  it('treats th:* as the attribute it renders', async () => {
    expect(await lint('<img src="a.png" />')).toEqual(['wcag/h37'])
    expect(await lint('<img src="a.png" th:alt="${alt}" />')).toEqual([])
  })

  it('reads the attribute names out of th:attr', async () => {
    const markup = '<img th:attr="src=${url},alt=${alt}" />'

    expect(await lint(markup)).toEqual([])
  })

  it('reads both attributes out of th:alt-title', async () => {
    expect(await lint('<img src="a.png" th:alt-title="${alt}" />')).toEqual([])
  })

  it('does not treat th:if as an html attribute', async () => {
    // `if` is not a valid attribute, so mapping it would be reported.
    expect(await lint('<p th:if="${show}">text</p>')).toEqual([])
  })
})

describe('data-th-* attributes', () => {
  it('treats data-th-* as the attribute it renders', async () => {
    expect(await lint('<img src="x" data-th-alt="x" />')).toEqual([])
    expect(await lint('<img data-th-attr="src=${url},alt=${alt}" />')).toEqual(
      [],
    )
  })

  it('counts data-th-text as content', async () => {
    expect(await lint('<h2 data-th-text="${title}"></h2>')).toEqual([])
  })

  it('treats a data-th-replace host like a th:replace host', async () => {
    const markup = `
      <!doctype html>
      <html lang="en">
        <head data-th-replace="~{a :: b}"></head>
        <body></body>
      </html>`

    expect(await lint(markup)).toEqual([])
  })

  it('passes data-th-* names on to the other rules', async () => {
    expect(await lint('<div data-th-hx-swaap="${swap}"></div>')).toEqual([
      'civiform/htmx-attributes',
    ])
    expect(
      await lint('<button type="button" data-th-onclick="${go}">Go</button>'),
    ).toEqual(['civiform/no-inline-script'])
  })
})

describe('thymeleaf content', () => {
  it('counts th:text as content', async () => {
    expect(await lint('<h2></h2>')).toEqual(['empty-heading'])
    expect(await lint('<h2 th:text="${title}"></h2>')).toEqual([])
  })

  it('counts a th:replace fragment as content', async () => {
    const markup = '<h2><th:block th:replace="~{a :: b}"></th:block></h2>'

    expect(await lint(markup)).toEqual([])
  })

  it('does not check the content of a th:replace host', async () => {
    const page = (head) =>
      `<!doctype html><html lang="en">${head}<body></body></html>`

    expect(await lint(page('<head></head>'))).toEqual([
      'element-required-content',
    ])
    expect(await lint(page('<head th:replace="~{a :: b}"></head>'))).toEqual([])
  })

  it('does not give a th:replace host a landmark role', async () => {
    const page = (nav) => `<main>${nav}${nav}</main>`

    expect(await lint(page('<nav>a</nav>'))).toContain('unique-landmark')
    expect(await lint(page('<nav th:replace="~{a :: b}"></nav>'))).toEqual([])
  })

  it('still checks where a th:replace host is placed', async () => {
    const markup = '<ul><div th:replace="~{a :: b}"></div></ul>'

    expect(await lint(markup)).toEqual(['element-permitted-content'])
  })
})

describe('th:block', () => {
  it('is allowed where only specific children are', async () => {
    const markup = `
      <ul>
        <th:block th:each="item : \${items}">
          <li th:text="\${item}"></li>
        </th:block>
      </ul>`

    expect(await lint(markup)).toEqual([])
  })

  it('checks its children against its parent', async () => {
    const markup = '<ul><th:block th:if="${show}"><div>a</div></th:block></ul>'

    expect(await lint(markup)).toEqual(['element-permitted-content'])
  })

  it('can be the root of a fragment of list items', async () => {
    expect(await lint('<div><li>a</li></div>')).toContain(
      'element-permitted-parent',
    )
    expect(
      await lint('<th:block th:fragment="item"><li>a</li></th:block>'),
    ).toEqual([])
  })
})

describe('cf tags', () => {
  it('accepts valid cf:* tags', async () => {
    const markup = `
      <cf:input id="email" name="email" label="Email" type="email" size="md" />
      <cf:checkbox id="colors" name="colors" label="Colors" columns="2">
        <item id="red" label="Red" value="red"></item>
      </cf:checkbox>
      <cf:button type="submit" variant="outline"
        ><cf:icon type="icon-add" />Save</cf:button
      >`

    expect(await lint(markup)).toEqual([])
  })

  it('takes th:* for the required attributes', async () => {
    const markup = `
      <cf:radio th:id="\${id}" th:name="\${name}" th:label="#{label}">
        <item
          th:each="item : \${items}"
          th:id="\${item.id()}"
          th:label="\${item.label()}"
          th:value="\${item.value()}"
        ></item>
      </cf:radio>
      <cf:icon th:type="\${icon}" />`

    expect(await lint(markup)).toEqual([])
  })

  it('requires id, name and label on a form control', async () => {
    expect(await lint('<cf:input id="email" label="Email" />')).toEqual([
      'element-required-attributes',
    ])
    expect(
      await lint('<cf:select name="state" label="State"></cf:select>'),
    ).toEqual(['element-required-attributes'])
    expect(
      await lint('<cf:textarea id="notes" name="notes"></cf:textarea>'),
    ).toEqual(['element-required-attributes'])
  })

  it('rejects a name or label the dialect would reject', async () => {
    expect(
      await lint('<cf:input id="first" name="first name" label="First" />'),
    ).toEqual(['attribute-allowed-values'])
    expect(
      await lint('<cf:input id="first" name="first" label=" " />'),
    ).toEqual(['attribute-allowed-values'])
  })

  it('rejects an input type the dialect does not render', async () => {
    const markup =
      '<cf:input id="agree" name="agree" label="Agree" type="checkbox" />'

    expect(await lint(markup)).toEqual(['attribute-allowed-values'])
  })

  it('needs validation-class and validation-field together', async () => {
    const input = (attributes) =>
      `<cf:input id="email" name="email" label="Email" ${attributes} />`

    expect(await lint(input('validation-class="forms.EmailForm"'))).toEqual([
      'attribute-misuse',
    ])
    expect(await lint(input('validation-field="email"'))).toEqual([
      'attribute-misuse',
    ])
    expect(
      await lint(
        input('validation-class="forms.EmailForm" validation-field="email"'),
      ),
    ).toEqual([])
  })

  it('rejects button attributes the dialect would reject', async () => {
    expect(await lint('<cf:button type="sumbit">Save</cf:button>')).toEqual([
      'attribute-allowed-values',
    ])
    expect(await lint('<cf:button variant="primary">Save</cf:button>')).toEqual(
      ['attribute-allowed-values'],
    )
    expect(await lint('<cf:button size="small">Save</cf:button>')).toEqual([
      'attribute-allowed-values',
    ])
  })

  it('requires an icon type that has a file', async () => {
    expect(await lint('<cf:icon />')).toEqual(['element-required-attributes'])
    expect(await lint('<cf:icon type="icon-missing" />')).toEqual([
      'attribute-allowed-values',
    ])
  })

  it('requires id, label and value on an item', async () => {
    const markup = `
      <cf:radio id="size" name="size" label="Size">
        <item id="small" label="Small"></item>
      </cf:radio>`

    expect(await lint(markup)).toEqual(['element-required-attributes'])
  })

  it('only allows items inside an option group', async () => {
    const markup = `
      <cf:checkbox id="colors" name="colors" label="Colors">
        <option value="red">Red</option>
      </cf:checkbox>`

    expect(await lint(markup)).toEqual(['element-permitted-content'])
  })

  it('only allows an item inside an option group', async () => {
    const markup = '<div><item id="red" label="Red" value="red"></item></div>'

    expect(await lint(markup)).toEqual(['element-permitted-content'])
  })

  it('reports a tag the dialect does not have', async () => {
    expect(await lint('<cf:typo />')).toEqual(['no-unknown-elements'])
  })
})

describe('directives', () => {
  it('reads a directive from a thymeleaf parser-level comment', async () => {
    const markup = `
      <!--/* [html-validate-disable-next empty-heading -- filled in by script] */-->
      <h2></h2>`

    expect(await lint(markup)).toEqual([])
  })

  it('reports a directive that suppresses nothing', async () => {
    const markup = `
      <!--/* [html-validate-disable-next empty-heading -- stale] */-->
      <h2>Title</h2>`

    expect(await lint(markup)).toEqual(['no-unused-disable'])
  })
})
