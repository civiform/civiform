import {FeatureCollection} from 'geojson'
import {LngLatLike, StyleSpecification} from 'maplibre-gl'

export interface MapData {
  geoJson: FeatureCollection
  settings: {
    nameGeoJsonKey: string
    addressGeoJsonKey: string
    detailsUrlGeoJsonKey: string
  }
}

// CSS SELECTORS
export const CF_LOCATIONS_LIST_CONTAINER = 'cf-locations-list'
export const CF_LOCATION_CHECKBOX = 'cf-location-checkbox'
export const CF_POPUP_CONTENT_TEMPLATE = 'cf-popup-content-template'
export const CF_NO_SELECTIONS_MESSAGE = 'cf-no-selections-message'
export const CF_SELECTED_LOCATIONS_LIST = 'cf-selected-locations-list'
export const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE =
  'cf-selected-location-checkbox-template'
export const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_INPUT =
  '.cf-selected-location-checkbox-template-input'
export const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LABEL =
  'cf-selected-location-checkbox-template-label'

// MAP DEFAULTS
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

export const queryMapSelectOptions = (mapId: string): NodeListOf<Element> => {
  return document.querySelectorAll(`select[data-map-id="${mapId}"]`)
}

export const queryLocationCheckboxes = (mapId: string) => {
  const locationsListContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (!locationsListContainer) return []

  return locationsListContainer.querySelectorAll(`.${CF_LOCATION_CHECKBOX}`)
}
