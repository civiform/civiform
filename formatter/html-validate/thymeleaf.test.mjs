import assert from 'node:assert/strict'
import {describe, it} from 'node:test'
import {lint} from './testing.mjs'

describe('thymeleaf attributes', () => {
  it('treats th:* as the attribute it renders', async () => {
    assert.deepEqual(await lint('<img src="a.png" />'), ['wcag/h37'])
    assert.deepEqual(await lint('<img src="a.png" th:alt="${alt}" />'), [])
  })

  it('reads the attribute names out of th:attr', async () => {
    const markup = '<img th:attr="src=${url},alt=${alt}" />'

    assert.deepEqual(await lint(markup), [])
  })

  it('reads both attributes out of th:alt-title', async () => {
    assert.deepEqual(
      await lint('<img src="a.png" th:alt-title="${alt}" />'),
      [],
    )
  })

  it('does not treat th:if as an html attribute', async () => {
    // `if` is not a valid attribute, so mapping it would be reported.
    assert.deepEqual(await lint('<p th:if="${show}">text</p>'), [])
  })
})

describe('data-th-* attributes', () => {
  it('treats data-th-* as the attribute it renders', async () => {
    assert.deepEqual(await lint('<img src="x" data-th-alt="x" />'), [])
    assert.deepEqual(
      await lint('<img data-th-attr="src=${url},alt=${alt}" />'),
      [],
    )
  })

  it('counts data-th-text as content', async () => {
    assert.deepEqual(await lint('<h2 data-th-text="${title}"></h2>'), [])
  })

  it('treats a data-th-replace host like a th:replace host', async () => {
    const markup = `
      <!doctype html>
      <html lang="en">
        <head data-th-replace="~{a :: b}"></head>
        <body></body>
      </html>`

    assert.deepEqual(await lint(markup), [])
  })

  it('passes data-th-* names on to the other rules', async () => {
    assert.deepEqual(await lint('<div data-th-hx-swaap="${swap}"></div>'), [
      'civiform/htmx-attributes',
    ])
    assert.deepEqual(
      await lint('<button type="button" data-th-onclick="${go}">Go</button>'),
      ['civiform/no-inline-script'],
    )
  })
})

describe('thymeleaf content', () => {
  it('counts th:text as content', async () => {
    assert.deepEqual(await lint('<h2></h2>'), ['empty-heading'])
    assert.deepEqual(await lint('<h2 th:text="${title}"></h2>'), [])
  })

  it('counts a th:replace fragment as content', async () => {
    const markup = '<h2><th:block th:replace="~{a :: b}"></th:block></h2>'

    assert.deepEqual(await lint(markup), [])
  })

  it('does not check the content of a th:replace host', async () => {
    const page = (head) =>
      `<!doctype html><html lang="en">${head}<body></body></html>`

    assert.deepEqual(await lint(page('<head></head>')), [
      'element-required-content',
    ])
    assert.deepEqual(
      await lint(page('<head th:replace="~{a :: b}"></head>')),
      [],
    )
  })

  it('does not give a th:replace host a landmark role', async () => {
    const page = (nav) => `<main>${nav}${nav}</main>`

    assert.ok((await lint(page('<nav>a</nav>'))).includes('unique-landmark'))
    assert.deepEqual(await lint(page('<nav th:replace="~{a :: b}"></nav>')), [])
  })

  it('still checks where a th:replace host is placed', async () => {
    const markup = '<ul><div th:replace="~{a :: b}"></div></ul>'

    assert.deepEqual(await lint(markup), ['element-permitted-content'])
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

    assert.deepEqual(await lint(markup), [])
  })

  it('checks its children against its parent', async () => {
    const markup = '<ul><th:block th:if="${show}"><div>a</div></th:block></ul>'

    assert.deepEqual(await lint(markup), ['element-permitted-content'])
  })

  it('can be the root of a fragment of list items', async () => {
    assert.ok(
      (await lint('<div><li>a</li></div>')).includes(
        'element-permitted-parent',
      ),
    )
    assert.deepEqual(
      await lint('<th:block th:fragment="item"><li>a</li></th:block>'),
      [],
    )
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

    assert.deepEqual(await lint(markup), [])
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

    assert.deepEqual(await lint(markup), [])
  })

  it('requires id, name and label on a form control', async () => {
    assert.deepEqual(await lint('<cf:input id="email" label="Email" />'), [
      'element-required-attributes',
    ])
    assert.deepEqual(
      await lint('<cf:select name="state" label="State"></cf:select>'),
      ['element-required-attributes'],
    )
    assert.deepEqual(
      await lint('<cf:textarea id="notes" name="notes"></cf:textarea>'),
      ['element-required-attributes'],
    )
  })

  it('rejects a name or label the dialect would reject', async () => {
    assert.deepEqual(
      await lint('<cf:input id="first" name="first name" label="First" />'),
      ['attribute-allowed-values'],
    )
    assert.deepEqual(
      await lint('<cf:input id="first" name="first" label=" " />'),
      ['attribute-allowed-values'],
    )
  })

  it('rejects an input type the dialect does not render', async () => {
    const markup =
      '<cf:input id="agree" name="agree" label="Agree" type="checkbox" />'

    assert.deepEqual(await lint(markup), ['attribute-allowed-values'])
  })

  it('needs validation-class and validation-field together', async () => {
    const input = (attributes) =>
      `<cf:input id="email" name="email" label="Email" ${attributes} />`

    assert.deepEqual(await lint(input('validation-class="forms.EmailForm"')), [
      'attribute-misuse',
    ])
    assert.deepEqual(await lint(input('validation-field="email"')), [
      'attribute-misuse',
    ])
    assert.deepEqual(
      await lint(
        input('validation-class="forms.EmailForm" validation-field="email"'),
      ),
      [],
    )
  })

  it('only takes "true" or "false" for a flag', async () => {
    const input = (attributes) =>
      `<cf:input id="email" name="email" label="Email" ${attributes} />`

    assert.deepEqual(await lint(input('required="true" is-valid="false"')), [])
    assert.deepEqual(await lint(input('th:required="${required}"')), [])
    // The dialect reads both of these as false, so the input isn't required.
    assert.deepEqual(await lint(input('required')), [
      'attribute-allowed-values',
    ])
    assert.deepEqual(await lint(input('required="required"')), [
      'attribute-allowed-values',
    ])
  })

  it('rejects button attributes the dialect would reject', async () => {
    assert.deepEqual(await lint('<cf:button type="sumbit">Save</cf:button>'), [
      'attribute-allowed-values',
    ])
    assert.deepEqual(
      await lint('<cf:button variant="primary">Save</cf:button>'),
      ['attribute-allowed-values'],
    )
    assert.deepEqual(await lint('<cf:button size="small">Save</cf:button>'), [
      'attribute-allowed-values',
    ])
  })

  it('requires an icon type that has a file', async () => {
    assert.deepEqual(await lint('<cf:icon />'), ['element-required-attributes'])
    assert.deepEqual(await lint('<cf:icon type="icon-missing" />'), [
      'attribute-allowed-values',
    ])
  })

  it('requires id, label and value on an item', async () => {
    const markup = `
      <cf:radio id="size" name="size" label="Size">
        <item id="small" label="Small"></item>
      </cf:radio>`

    assert.deepEqual(await lint(markup), ['element-required-attributes'])
  })

  it('only allows items inside an option group', async () => {
    const markup = `
      <cf:checkbox id="colors" name="colors" label="Colors">
        <option value="red">Red</option>
      </cf:checkbox>`

    assert.deepEqual(await lint(markup), ['element-permitted-content'])
  })

  it('only allows an item inside an option group', async () => {
    const markup = '<div><item id="red" label="Red" value="red"></item></div>'

    assert.deepEqual(await lint(markup), ['element-permitted-content'])
  })

  it('reports a tag the dialect does not have', async () => {
    assert.deepEqual(await lint('<cf:typo />'), ['no-unknown-elements'])
  })
})

describe('directives', () => {
  it('reads a directive from a thymeleaf parser-level comment', async () => {
    const markup = `
      <!--/* [html-validate-disable-next empty-heading -- filled in by script] */-->
      <h2></h2>`

    assert.deepEqual(await lint(markup), [])
  })

  it('reports a directive that suppresses nothing', async () => {
    const markup = `
      <!--/* [html-validate-disable-next empty-heading -- stale] */-->
      <h2>Title</h2>`

    assert.deepEqual(await lint(markup), ['no-unused-disable'])
  })
})
