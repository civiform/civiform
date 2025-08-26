import {FeatureCollection} from 'geojson'
import {LngLatLike, StyleSpecification} from 'maplibre-gl'

export interface MapSettings {
  readonly nameGeoJsonKey: string
  readonly addressGeoJsonKey: string
  readonly detailsUrlGeoJsonKey: string
}

export interface MapData {
  geoJson: FeatureCollection
  settings: MapSettings
}

// CSS SELECTORS
export const CF_POPUP_CONTENT_TEMPLATE = 'cf-popup-content-template'
export const CF_POPUP_CONTENT_LOCATION_NAME = 'cf-popup-content-location-name'
export const CF_POPUP_CONTENT_LOCATION_ADDRESS =
  'cf-popup-content-location-address'
export const CF_POPUP_CONTENT_LOCATION_LINK_CONTAINER =
  'cf-popup-content-location-link-container'
export const CF_POPUP_CONTENT_LOCATION_LINK = 'cf-popup-content-location-link'

// MAP DEFAULTS
export const LOCATIONS_SOURCE = 'locations'
export const LOCATIONS_LAYER = 'locations-layer'
export const DEFAULT_MAP_CENTER_POINT: LngLatLike = [-122.3321, 47.6062]
export const DEFAULT_MAP_ZOOM = 8
export const DEFAULT_MAP_MARKER_TYPE = 'circle'
export const DEFAULT_MAP_MARKER_STYLE = {
  'circle-radius': 6,
  'circle-color': '#005EA2',
}
export const DEFAULT_MAP_STYLE: StyleSpecification = {
  version: 8,
  sources: {
    osm: {
      type: 'raster',
      tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
      tileSize: 256,
      attribution: '© OpenStreetMap contributors',
    },
  },
  layers: [{id: 'osm', type: 'raster', source: 'osm'}],
}

// Query elements within a specific map container
export const mapQuerySelector = (
  mapId: string,
  className: string,
): Element | null => {
  return document.querySelector(`[data-map-id="${mapId}"].${className}`)
}
