import {FeatureCollection} from 'geojson'
import {calculateMapCenter} from './map_util'

describe('calculateMapCenter', () => {
  it('returns null for empty FeatureCollection', () => {
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [],
    }

    const result = calculateMapCenter(geoJson)

    expect(result).toBeNull()
  })

  it('returns null for FeatureCollection with no Point features', () => {
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: {
            type: 'LineString',
            coordinates: [
              [-122.3321, 47.6062],
              [-122.3233, 47.5979],
            ],
          },
          properties: {},
        },
      ],
    }

    const result = calculateMapCenter(geoJson)

    expect(result).toBeNull()
  })

  it('calculates center for single Point feature', () => {
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [-122.3321, 47.6062],
          },
          properties: {
            name: 'Seattle Central Library',
          },
        },
      ],
    }

    const result = calculateMapCenter(geoJson)

    expect(result).toEqual([-122.3321, 47.6062])
  })

  it('calculates center for multiple Point features', () => {
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [-122.3321, 47.6062], // Seattle Central Library
          },
          properties: {name: 'Seattle Central Library'},
        },
        {
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [-122.3233, 47.5979], // International District
          },
          properties: {name: 'International District Community Center'},
        },
      ],
    }

    const result = calculateMapCenter(geoJson)

    // Expected center: [(-122.3321 + -122.3233) / 2, (47.6062 + 47.5979) / 2]
    // = [-122.3277, 47.60205]
    expect(result).not.toBeNull()
    const [lng, lat] = result as [number, number]
    expect(lng).toBeCloseTo(-122.3277, 5)
    expect(lat).toBeCloseTo(47.60205, 5)
  })

  it('calculates center for features at extreme coordinates', () => {
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [-122.4, 47.7], // Northwest corner
          },
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [-122.2, 47.5], // Southeast corner
          },
          properties: {},
        },
      ],
    }

    const result = calculateMapCenter(geoJson)

    // Expected center: [(-122.4 + -122.2) / 2, (47.7 + 47.5) / 2]
    // = [-122.3, 47.6]
    expect(result).not.toBeNull()
    const [lng, lat] = result as [number, number]
    expect(lng).toBeCloseTo(-122.3, 5)
    expect(lat).toBeCloseTo(47.6, 5)
  })

  it('calculates center correctly with multiple scattered locations', () => {
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-122.3321, 47.6062]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-122.3233, 47.5979]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-122.3207, 47.6205]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-122.3817, 47.6677]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-122.315, 47.6606]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-122.2619, 47.5223]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-122.3985, 47.6417]},
          properties: {},
        },
      ],
    }

    const result = calculateMapCenter(geoJson)

    // Calculate expected bounding box center:
    // minLng: -122.3985, maxLng: -122.2619
    // minLat: 47.5223, maxLat: 47.6677
    // center: [(-122.3985 + -122.2619) / 2, (47.5223 + 47.6677) / 2]
    // = [-122.3302, 47.595]
    expect(result).not.toBeNull()
    const [lng, lat] = result as [number, number]
    expect(lng).toBeCloseTo(-122.3302, 5)
    expect(lat).toBeCloseTo(47.595, 5)
  })

  it('ignores invalid coordinates', () => {
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [-122.3321, 47.6062],
          },
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {
            type: 'Point',
            // @ts-expect-error Testing invalid coordinates
            coordinates: ['invalid', 'coords'],
          },
          properties: {},
        },
      ],
    }

    const result = calculateMapCenter(geoJson)

    // Should only use the valid coordinate
    expect(result).toEqual([-122.3321, 47.6062])
  })

  it('returns null when all coordinates are invalid', () => {
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: {
            type: 'Point',
            // @ts-expect-error Testing invalid coordinates
            coordinates: ['invalid', 'coords'],
          },
          properties: {},
        },
      ],
    }

    const result = calculateMapCenter(geoJson)

    expect(result).toBeNull()
  })
})
