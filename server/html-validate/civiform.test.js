// @vitest-environment node
import {describe, expect, it} from 'vitest'
import {lint, lintMessages} from './testing.js'

describe('civiform/form-submit', () => {
  const RULE = 'civiform/form-submit'

  it('reports a form without a submit button', async () => {
    const markup = `
      <form method="post" action="/save">
        <button type="button">Cancel</button>
        <cf:button>Also not a submit button</cf:button>
      </form>`

    expect(await lint(markup)).toEqual([RULE])
  })

  it('accepts a cf:button that submits', async () => {
    const form = (button) =>
      `<form method="post" action="/save">${button}</form>`

    expect(
      await lint(form('<cf:button type="submit">Save</cf:button>')),
    ).toEqual([])
    expect(
      await lint(form('<cf:button th:type="${type}">Save</cf:button>')),
    ).toEqual([])
  })

  it('accepts a native submit control', async () => {
    const form = (button) =>
      `<form method="post" action="/save">${button}</form>`

    expect(await lint(form('<button type="submit">Save</button>'))).toEqual([])
    expect(
      await lint(
        form(
          '<!--/* [html-validate-disable-next prefer-button -- test] */--><input type="submit" value="Save" />',
        ),
      ),
    ).toEqual([])
  })

  it('accepts a submit button that names the form', async () => {
    const markup = `
      <form id="save-form" method="post" action="/save"></form>
      <cf:button type="submit" form="save-form">Save</cf:button>`

    expect(await lint(markup)).toEqual([])
  })

  it('accepts a form that includes a fragment', async () => {
    const markup = `
      <form method="post" action="/save">
        <th:block th:replace="~{buttons :: save}"></th:block>
      </form>`

    expect(await lint(markup)).toEqual([])
    expect(await lint('<form th:replace="~{forms :: save}"></form>')).toEqual(
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

    expect(await lint(markup)).toEqual([])
  })

  it('accepts th:hx-* and attributes set through th:attr', async () => {
    const markup = `
      <div
        th:hx-get="\${url}"
        th:hx-swap="\${swap}"
        th:attr="hx-vals=\${vals},hx-target=\${target}"
      ></div>`

    expect(await lint(markup)).toEqual([])
  })

  it('reports a misspelled attribute', async () => {
    const messages = await lintMessages('<div hx-swaap="outerHTML"></div>')

    expect(messages.map((message) => message.ruleId)).toEqual([RULE])
    expect(messages[0].message).toBe('Unknown htmx attribute "hx-swaap"')
  })

  it('reports a misspelled th:hx-* attribute', async () => {
    const messages = await lintMessages('<div th:hx-swaap="${swap}"></div>')

    expect(messages.map((message) => message.ruleId)).toEqual([RULE])
    expect(messages[0].message).toBe('Unknown htmx attribute "th:hx-swaap"')
  })

  it('reports a misspelled attribute set through th:attr', async () => {
    expect(await lint('<div th:attr="hx-psot=${url}"></div>')).toEqual([RULE])
  })

  it('reports a misspelled swap style', async () => {
    const messages = await lintMessages('<div hx-swap="outerHTLM"></div>')

    expect(messages.map((message) => message.ruleId)).toEqual([RULE])
    expect(messages[0].message).toContain('"outerHTLM" is not a valid hx-swap')
  })

  it('reports a swap style that is not first, and unknown modifiers', async () => {
    expect(await lint('<div hx-swap="show:top innerHTML"></div>')).toEqual([
      RULE,
    ])
    expect(await lint('<div hx-swap="innerHTML sroll:top"></div>')).toEqual([
      RULE,
    ])
  })

  it('accepts modifiers without a swap style', async () => {
    expect(await lint('<div hx-swap="swap:1s"></div>')).toEqual([])
  })
})

describe('civiform/no-inline-script', () => {
  const RULE = 'civiform/no-inline-script'

  it('reports an inline event handler', async () => {
    const messages = await lintMessages(
      '<select name="sort" onchange="this.form.submit()"></select>',
    )

    expect(messages.map((message) => message.ruleId)).toEqual([RULE])
    expect(messages[0].message).toContain('Inline event handler "onchange"')
  })

  it('reports a th:on* handler once, under the name it was written with', async () => {
    const messages = await lintMessages(
      '<button type="button" th:onclick="|go(${id})|">Go</button>',
    )

    expect(messages.map((message) => message.ruleId)).toEqual([RULE])
    expect(messages[0].message).toContain('Inline event handler "th:onclick"')
  })

  it('reports a handler set through th:attr', async () => {
    const markup =
      '<button type="button" th:attr="onclick=${handler}">Go</button>'

    expect(await lint(markup)).toEqual([RULE])
  })

  it('reports an htmx hx-on handler', async () => {
    const markup = '<button type="button" hx-on:click="go()">Go</button>'

    expect(await lint(markup)).toEqual([RULE])
  })

  it('reports a javascript: URL', async () => {
    expect(await lint('<a href="javascript:void(0)">Go</a>')).toEqual([RULE])
    expect(await lint('<a th:href="|javascript:go(${id})|">Go</a>')).toEqual([
      RULE,
    ])
  })

  it('accepts a javascript: placeholder that th:href overwrites', async () => {
    const markup = '<a href="javascript:void(0);" th:href="${url}">Go</a>'

    expect(await lint(markup)).toEqual([])
  })

  it('skips an element that th:replace swaps out', async () => {
    const markup = '<div onclick="go()" th:replace="~{a :: b}"></div>'

    expect(await lint(markup)).toEqual([])
  })

  it('accepts ordinary attributes and URLs', async () => {
    const markup =
      '<a href="/programs" th:href="${url}" data-on-load="x">Go</a>'

    expect(await lint(markup)).toEqual([])
  })
})
