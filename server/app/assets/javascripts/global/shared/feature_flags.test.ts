import {describe, it, expect, afterEach, vi} from 'vitest'

function setFlagsElement(content: string) {
  const el = document.createElement('script')
  el.id = 'feature-flags-data'
  el.type = 'application/json'
  el.textContent = content
  document.head.appendChild(el)
}

function loadModule() {
  vi.resetModules()
  return import('./feature_flags')
}

describe('featureFlags', () => {
  afterEach(() => {
    document.getElementById('feature-flags-data')?.remove()
  })

  it('throws when the script tag is missing', async () => {
    const {featureFlags} = await loadModule()
    expect(() => featureFlags()).toThrow(
      '#feature-flags-data script tag is missing or empty',
    )
  })

  it('throws when the script tag is empty', async () => {
    setFlagsElement('')
    const {featureFlags} = await loadModule()
    expect(() => featureFlags()).toThrow(
      '#feature-flags-data script tag is missing or empty',
    )
  })

  it('parses valid flags', async () => {
    const {featureFlags, DEFAULTS} = await loadModule()
    const allTrue = Object.fromEntries(
      Object.keys(DEFAULTS).map((k) => [k, true]),
    )
    setFlagsElement(JSON.stringify(allTrue))
    expect(featureFlags()).toEqual(allTrue)
  })

  it('freezes the result', async () => {
    const {featureFlags, DEFAULTS} = await loadModule()
    setFlagsElement(JSON.stringify(DEFAULTS))
    expect(Object.isFrozen(featureFlags())).toBe(true)
  })

  it('caches the result across calls', async () => {
    const {featureFlags, DEFAULTS} = await loadModule()
    setFlagsElement(JSON.stringify(DEFAULTS))
    expect(featureFlags()).toBe(featureFlags())
  })

  it('throws on invalid JSON', async () => {
    setFlagsElement('not json')
    const {featureFlags} = await loadModule()
    expect(() => featureFlags()).toThrow()
  })

  it('throws when JSON is not an object', async () => {
    setFlagsElement('"a string"')
    const {featureFlags} = await loadModule()
    expect(() => featureFlags()).toThrow('Feature flags JSON is not an object')
  })

  it('throws when JSON is null', async () => {
    setFlagsElement('null')
    const {featureFlags} = await loadModule()
    expect(() => featureFlags()).toThrow('Feature flags JSON is not an object')
  })

  it('throws when a flag is missing from the server response', async () => {
    const {featureFlags, DEFAULTS} = await loadModule()
    const [firstKey] = Object.keys(DEFAULTS)
    setFlagsElement(JSON.stringify({[firstKey]: true}))
    expect(() => featureFlags()).toThrow('missing from the server response')
  })

  it('throws when extra flags are in the server response', async () => {
    const {featureFlags, DEFAULTS} = await loadModule()
    setFlagsElement(JSON.stringify({...DEFAULTS, unexpected: true}))
    expect(() => featureFlags()).toThrow('not defined in the client')
  })
})
