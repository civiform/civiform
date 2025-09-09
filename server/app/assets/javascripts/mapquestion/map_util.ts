import {FeatureCollection} from 'geojson'
import {LngLatLike, StyleSpecification} from 'maplibre-gl'

export interface MapSettings {
  readonly nameGeoJsonKey: string
  readonly addressGeoJsonKey: string
  readonly detailsUrlGeoJsonKey: string
}

export interface MapMessages {
  readonly locationsCount: string
}

export interface MapData {
  geoJson: FeatureCollection
  settings: MapSettings
  messages: MapMessages
}

// DATA ATTRIBUTES
export const DATA_MAP_ID = 'data-map-id'
export const DATA_FEATURE_ID = 'data-feature-id'
export const DATA_FILTER_KEY = 'data-filter-key'

// POPUPS
export const CF_POPUP_CONTENT_TEMPLATE = 'cf-popup-content-template'
export const CF_POPUP_CONTENT_LOCATION_NAME = 'cf-popup-content-location-name'
export const CF_POPUP_CONTENT_LOCATION_ADDRESS =
  'cf-popup-content-location-address'
export const CF_POPUP_CONTENT_LOCATION_LINK = 'cf-popup-content-location-link'
export const CF_POPUP_CONTENT_BUTTON = 'cf-select-location-button'

// LOCATIONS LIST
export const CF_LOCATIONS_LIST_CONTAINER = 'cf-locations-list'
export const CF_LOCATION_CHECKBOX = 'cf-location-checkbox'
export const CF_LOCATION_CHECKBOX_INPUT = 'cf-location-checkbox-input'
export const CF_SELECTED_LOCATIONS_CONTAINER = 'cf-selected-locations-container'
export const CF_NO_SELECTIONS_MESSAGE = 'cf-no-selections-message'
export const DATA_FEATURE_ID_ATTR = 'data-feature-id'
export const DATA_MAP_ID_ATTR = 'data-map-id'

// FILTERS
export const CF_APPLY_FILTERS_BUTTON = 'cf-apply-filters-button'
export const CF_RESET_FILTERS_BUTTON = 'cf-reset-filters-button'
export const CF_LOCATION_COUNT = 'cf-location-count'
export const CF_LOCATION_HIDDEN = 'cf-location-hidden'

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
  return document.querySelector(`[${DATA_MAP_ID}="${mapId}"].${className}`)
}
