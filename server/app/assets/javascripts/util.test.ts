import {addEventListenerToElements, assertNotNull} from './util'

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
