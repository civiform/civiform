import {addEventListenerToElements} from './util'

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
    addEventListenerToElements('.marked', 'my-event', () => {
      listenerCalled++
    })

    firstDiv.dispatchEvent(new CustomEvent('my-event'))
    expect(listenerCalled).toBe(1)

    secondDiv.dispatchEvent(new CustomEvent('my-event'))
    // Second div should not have the listener registered and invoked.
    expect(listenerCalled).toBe(1)

    thirdDiv.dispatchEvent(new CustomEvent('my-event'))
    expect(listenerCalled).toBe(2)
  })
})
