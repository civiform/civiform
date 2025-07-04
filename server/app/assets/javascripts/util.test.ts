import {addEventListenerToElements, assertNotNull, formatTextHtml} from './util'

describe('addEventListenerToElements', () => {
  let container: HTMLElement

  beforeEach(() => {
    container = document.createElement('div')
    document.body.appendChild(container)
  })
  afterEach(() => {
    container.remove()
  })

  it('listeners registered correctly', () => {
    const firstDiv = document.createElement('div')
    firstDiv.classList.add('marked')
    container.appendChild(firstDiv)
    const secondDiv = document.createElement('div')
    container.appendChild(secondDiv)
    const thirdDiv = document.createElement('div')
    thirdDiv.classList.add('marked')
    container.appendChild(thirdDiv)

    let listenerCalled = 0
    addEventListenerToElements('.marked', 'click', () => {
      listenerCalled++
    })

    firstDiv.dispatchEvent(new CustomEvent('click'))
    expect(listenerCalled).toBe(1)

    secondDiv.dispatchEvent(new CustomEvent('click'))
    // Second div should not have the listener registered and invoked.
    expect(listenerCalled).toBe(1)

    thirdDiv.dispatchEvent(new CustomEvent('click'))
    expect(listenerCalled).toBe(2)
  })
})

describe('assert', () => {
  it('does not throw on non-null values', () => {
    expect(assertNotNull('')).toEqual('')
    expect(assertNotNull(false)).toEqual(false)
    expect(assertNotNull([])).toEqual([])
    expect(assertNotNull(0)).toEqual(0)
    const val = {}
    expect(assertNotNull(val)).toEqual(val)
  })

  it('throws errors on null and undefined', () => {
    expect(() => {
      assertNotNull(null)
    }).toThrow('Provided value is null.')
    expect(() => {
      assertNotNull(undefined)
    }).toThrow('Provided value is undefined.')
  })

  it('adds extra info to error if passed', () => {
    expect(() => {
      assertNotNull(null, 'foobar')
    }).toThrow('Provided value is null. Extra info: foobar')
  })
})

describe('formatTextHtml', () => {
  it('handles basic markdown', () => {
    const text =
      'this is some _text_ with __markdown__ and a [link](https://www.example.com)'
    expect(formatTextHtml(text).innerHTML).toContain(
      '<p>this is some <em>text</em> with <strong>markdown</strong> and a <a class="text-blue-600 hover:text-blue-500 underline" target="_blank" href="https://www.example.com">link</a></p>',
    )
  })

  it('respects blank lines', () => {
    const text = 'this is some\n text with \n\n line breaks'
    expect(formatTextHtml(text).innerHTML).toContain(
      '<p>this is some&nbsp;<br>\ntext with &nbsp;</p>\n<p>&nbsp;<br>\nline breaks</p>\n',
    )
  })

  it('automatically detects links and emails', () => {
    const text =
      'here is a url https://www.example.com and an email test@example.com'
    expect(formatTextHtml(text).innerHTML).toContain(
      '<p>here is a url <a class="text-blue-600 hover:text-blue-500 underline" target="_blank" href="https://www.example.com">https://www.example.com</a> and an email <a class="text-blue-600 hover:text-blue-500 underline" target="_blank" href="mailto:test@example.com">test@example.com</a></p>',
    )
  })

  it('adds formatting to lists', () => {
    const olText =
      'here is some markdown with an unordered list:\n - item one\n - item two\n - item 3'
    expect(formatTextHtml(olText).innerHTML).toContain(
      '<p>here is some markdown with an unordered list:&nbsp;</p>\n<ul class="list-disc mx-8">\n<li>item one&nbsp;</li>\n<li>item two&nbsp;</li>\n<li>item 3</li>\n</ul>\n',
    )
  })

  it('adds formatting to links', () => {
    const text = 'here is markdown with a [link](https://www.example.com)'
    expect(formatTextHtml(text).innerHTML).toContain(
      '<p>here is markdown with a <a class="text-blue-600 hover:text-blue-500 underline" target="_blank" href="https://www.example.com">link</a></p>',
    )
  })

  it('converts h1s to h2s', () => {
    const text = 'here is some markdown with an # h1 tag'
    expect(formatTextHtml(text).innerHTML).toContain(
      '<p>here is some markdown with an # h1 tag</p>',
    )
  })
})
