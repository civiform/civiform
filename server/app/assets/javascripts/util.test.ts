import {addEventListenerToElements, assert} from './util'

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
    expect(assert('')).toEqual('')
    expect(assert(false)).toEqual(false)
    expect(assert([])).toEqual([])
    expect(assert(0)).toEqual(0)
    const val = {}
    expect(assert(val)).toEqual(val)
  })

  it('throws errors on null and undefined', () => {
    expect(() => {
      assert(null)
    }).toThrow('Provided value is null.')
    expect(() => {
      assert(undefined)
    }).toThrow('Provided value is undefined.')
  })

  it('adds extra info to error if passed', () => {
    expect(() => {
      assert(null, 'foobar')
    }).toThrow('Provided value is null. Extra info: foobar')
  })
})
