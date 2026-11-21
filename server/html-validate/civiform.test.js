// @vitest-environment node
import {describe, expect, it} from 'vitest'
import {lint, lintMessages} from './testing.js'

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
