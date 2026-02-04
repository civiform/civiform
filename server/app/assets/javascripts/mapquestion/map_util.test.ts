import {describe, it, expect} from 'vitest'
import {FeatureCollection} from 'geojson'
import {calculateMapCenter} from '@/mapquestion/map_util'

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

    // Expected centroid (average): [(-122.3321 + -122.3233) / 2, (47.6062 + 47.5979) / 2]
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

    // Calculate expected centroid (average of all points):
    // Sum of longitudes: -856.3332, average: -122.3333
    // Sum of latitudes: 333.3169, average: 47.6167
    expect(result).not.toBeNull()
    const [lng, lat] = result as [number, number]
    expect(lng).toBeCloseTo(-122.3333, 4)
    expect(lat).toBeCloseTo(47.6167, 4)
  })

  it('demonstrates outlier resistance with centroid approach', () => {
    // 10 locations clustered in New York City area (~-74° longitude)
    // 1 outlier location in Los Angeles (~-118° longitude)
    const geoJson: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        // 10 NYC locations (clustered around -74.0, 40.7)
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-74.006, 40.7128]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-73.9857, 40.758]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-73.9712, 40.7831]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-74.0134, 40.7049]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-73.9626, 40.7614]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-74.006, 40.7489]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-73.9808, 40.7648]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-73.9937, 40.7295]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-74.0028, 40.7411]},
          properties: {},
        },
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-73.9903, 40.7359]},
          properties: {},
        },
        // 1 outlier in Los Angeles
        {
          type: 'Feature',
          geometry: {type: 'Point', coordinates: [-118.2437, 34.0522]},
          properties: {},
        },
      ],
    }

    const result = calculateMapCenter(geoJson)

    // With centroid (average), the outlier has limited impact:
    // 10 NYC points average ~-74.0, 1 LA point at -118.2
    // Centroid longitude ≈ (-74.0*10 + -118.2) / 11 ≈ -78.02
    // This keeps the center much closer to the NYC cluster
    //
    // With bounding box approach, center would be:
    // (min: -118.2, max: -73.96) / 2 ≈ -96.08 (middle of Kansas!)
    expect(result).not.toBeNull()
    const [lng, lat] = result as [number, number]
    // Center should be closer to NYC (~-74°) than to the midpoint with LA
    expect(lng).toBeGreaterThan(-80) // Much closer to NYC than Kansas
    expect(lng).toBeLessThan(-74) // But still pulled slightly west by LA outlier
    expect(lat).toBeGreaterThan(39) // Latitude should also be closer to NYC
    expect(lat).toBeLessThan(41)
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
