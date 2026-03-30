export const DEFAULTS = {
  isAdminUiMigrationScEnabled: false,
  isAdminUiMigrationScExtendedEnabled: false,
} as const

export type FeatureFlags = {readonly [K in keyof typeof DEFAULTS]: boolean}

function readServerFlags(): FeatureFlags {
  const rawJson = document.getElementById('feature-flags-data')?.textContent

  if (!rawJson) {
    throw new Error('#feature-flags-data script tag is missing or empty')
  }

  const parsed: unknown = JSON.parse(rawJson)

  if (typeof parsed !== 'object' || parsed === null) {
    throw new Error('Feature flags JSON is not an object')
  }

  const flagNames = Object.keys(DEFAULTS) as (keyof FeatureFlags)[]

  const missingFlags = flagNames.filter(
    (key) => typeof (parsed as Record<string, unknown>)[key] !== 'boolean',
  )

  const extraFlags = Object.keys(parsed).filter((key) => !(key in DEFAULTS))

  if (missingFlags.length > 0) {
    throw new Error(
      `Feature flag mismatch: the following flags are defined in the client but missing from the server response: ${missingFlags.join(', ')}.`,
    )
  }

  if (extraFlags.length > 0) {
    throw new Error(
      `Feature flag mismatch: the following flags were returned by the server but are not defined in the client: ${extraFlags.join(', ')}.`,
    )
  }

  return Object.freeze(parsed as FeatureFlags)
}

let _cachedFeatureFlags: FeatureFlags | null = null

// As soon as this is called we want to parse the json in the dom once. This prevents module load
// issues if imported multiple times and will cache the results for the page lifetime.
export function featureFlags(): FeatureFlags {
  if (!_cachedFeatureFlags) {
    _cachedFeatureFlags = readServerFlags()
  }
  return _cachedFeatureFlags
}
