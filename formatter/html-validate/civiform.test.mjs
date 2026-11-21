import assert from 'node:assert/strict'
import {describe, it} from 'node:test'
import {lint, lintMessages} from './testing.mjs'

describe('civiform/form-submit', () => {
  const RULE = 'civiform/form-submit'

  it('reports a form without a submit button', async () => {
    const markup = `
      <form method="post" action="/save">
        <button type="button">Cancel</button>
        <cf:button>Also not a submit button</cf:button>
      </form>`

    assert.deepEqual(await lint(markup), [RULE])
  })

  it('accepts a cf:button that submits', async () => {
    const form = (button) =>
      `<form method="post" action="/save">${button}</form>`

    assert.deepEqual(
      await lint(form('<cf:button type="submit">Save</cf:button>')),
      [],
    )
    assert.deepEqual(
      await lint(form('<cf:button th:type="${type}">Save</cf:button>')),
      [],
    )
  })

  it('accepts a native submit control', async () => {
    const form = (button) =>
      `<form method="post" action="/save">${button}</form>`

    assert.deepEqual(
      await lint(form('<button type="submit">Save</button>')),
      [],
    )
    assert.deepEqual(
      await lint(
        form(
          '<!--/* [html-validate-disable-next prefer-button -- test] */--><input type="submit" value="Save" />',
        ),
      ),
      [],
    )
  })

  it('accepts a submit button that names the form', async () => {
    const markup = `
      <form id="save-form" method="post" action="/save"></form>
      <cf:button type="submit" form="save-form">Save</cf:button>`

    assert.deepEqual(await lint(markup), [])
  })

  it('accepts a form that includes a fragment', async () => {
    const markup = `
      <form method="post" action="/save">
        <th:block th:replace="~{buttons :: save}"></th:block>
      </form>`

    assert.deepEqual(await lint(markup), [])
    assert.deepEqual(
      await lint('<form th:replace="~{forms :: save}"></form>'),
      [],
    )
  })
})

describe('civiform/htmx-attributes', () => {
  const RULE = 'civiform/htmx-attributes'

  it('accepts the attributes htmx reads', async () => {
    const markup = `
      <form
        hx-post="/save"
        hx-target="#result"
        hx-target-error="#errors"
        hx-target-4xx="#errors"
        hx-swap="innerHTML show:top"
        hx-ext="response-targets"
        data-hx-indicator="#spinner"
      >
        <button type="submit">Save</button>
      </form>`

    assert.deepEqual(await lint(markup), [])
  })

  it('accepts th:hx-* and attributes set through th:attr', async () => {
    const markup = `
      <div
        th:hx-get="\${url}"
        th:hx-swap="\${swap}"
        th:attr="hx-vals=\${vals},hx-target=\${target}"
      ></div>`

    assert.deepEqual(await lint(markup), [])
  })

  it('reports a misspelled attribute', async () => {
    const messages = await lintMessages('<div hx-swaap="outerHTML"></div>')

    assert.deepEqual(
      messages.map((message) => message.ruleId),
      [RULE],
    )
    assert.equal(messages[0].message, 'Unknown htmx attribute "hx-swaap"')
  })

  it('reports a misspelled th:hx-* attribute', async () => {
    const messages = await lintMessages('<div th:hx-swaap="${swap}"></div>')

    assert.deepEqual(
      messages.map((message) => message.ruleId),
      [RULE],
    )
    assert.equal(messages[0].message, 'Unknown htmx attribute "th:hx-swaap"')
  })

  it('reports a misspelled attribute set through th:attr', async () => {
    assert.deepEqual(await lint('<div th:attr="hx-psot=${url}"></div>'), [RULE])
  })

  it('reports a misspelled swap style', async () => {
    const messages = await lintMessages('<div hx-swap="outerHTLM"></div>')

    assert.deepEqual(
      messages.map((message) => message.ruleId),
      [RULE],
    )
    assert.ok(
      messages[0].message.includes('"outerHTLM" is not a valid hx-swap'),
    )
  })

  it('reports a swap style that is not first, and unknown modifiers', async () => {
    assert.deepEqual(await lint('<div hx-swap="show:top innerHTML"></div>'), [
      RULE,
    ])
    assert.deepEqual(await lint('<div hx-swap="innerHTML sroll:top"></div>'), [
      RULE,
    ])
  })

  it('accepts modifiers without a swap style', async () => {
    assert.deepEqual(await lint('<div hx-swap="swap:1s"></div>'), [])
  })
})

describe('civiform/no-inline-script', () => {
  const RULE = 'civiform/no-inline-script'

  it('reports an inline event handler', async () => {
    const messages = await lintMessages(
      '<select name="sort" onchange="this.form.submit()"></select>',
    )

    assert.deepEqual(
      messages.map((message) => message.ruleId),
      [RULE],
    )
    assert.ok(messages[0].message.includes('Inline event handler "onchange"'))
  })

  it('reports a th:on* handler once, under the name it was written with', async () => {
    const messages = await lintMessages(
      '<button type="button" th:onclick="|go(${id})|">Go</button>',
    )

    assert.deepEqual(
      messages.map((message) => message.ruleId),
      [RULE],
    )
    assert.ok(messages[0].message.includes('Inline event handler "th:onclick"'))
  })

  it('reports a handler set through th:attr', async () => {
    const markup =
      '<button type="button" th:attr="onclick=${handler}">Go</button>'

    assert.deepEqual(await lint(markup), [RULE])
  })

  it('reports an htmx hx-on handler', async () => {
    const markup = '<button type="button" hx-on:click="go()">Go</button>'

    assert.deepEqual(await lint(markup), [RULE])
  })

  it('reports a javascript: URL', async () => {
    assert.deepEqual(await lint('<a href="javascript:void(0)">Go</a>'), [RULE])
    assert.deepEqual(await lint('<a th:href="|javascript:go(${id})|">Go</a>'), [
      RULE,
    ])
  })

  it('accepts a javascript: placeholder that th:href overwrites', async () => {
    const markup = '<a href="javascript:void(0);" th:href="${url}">Go</a>'

    assert.deepEqual(await lint(markup), [])
  })

  it('skips an element that th:replace swaps out', async () => {
    const markup = '<div onclick="go()" th:replace="~{a :: b}"></div>'

    assert.deepEqual(await lint(markup), [])
  })

  it('accepts ordinary attributes and URLs', async () => {
    const markup =
      '<a href="/programs" th:href="${url}" data-on-load="x">Go</a>'

    assert.deepEqual(await lint(markup), [])
  })
})
