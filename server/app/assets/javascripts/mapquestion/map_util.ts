import {FeatureCollection} from 'geojson'
import {LngLatLike, StyleSpecification} from 'maplibre-gl'

export interface MapSettings {
  readonly nameGeoJsonKey: string
  readonly addressGeoJsonKey: string
  readonly detailsUrlGeoJsonKey: string
  readonly tagGeoJsonKey?: string
  readonly tagGeoJsonValue?: string
  readonly maxLocationSelections?: string
}

export interface MapMessages {
  readonly locationsCount: string
  readonly locationLinkTextSr: string
  readonly locationsSelectedCount: string
  readonly mapRegionAltText: string
  readonly goToPage: string
  readonly paginationStatus: string
  readonly mapSelectedButtonText: string
  readonly mapSelectLocationButtonText: string
  readonly switchViewUpdateSr: string
}

export interface MapData {
  geoJson: FeatureCollection
  settings: MapSettings
}

// DATA ATTRIBUTES
export const DATA_FEATURE_ID = 'data-feature-id'
export const DATA_FILTER_KEY = 'data-filter-key'
export const DATA_MAP_ID = 'data-map-id'

// POPUPS
export const CF_POPUP_CONTENT_BUTTON = 'cf-select-location-button'
export const CF_POPUP_CONTENT_LOCATION_ADDRESS =
  'cf-popup-content-location-address'
export const CF_POPUP_CONTENT_LOCATION_LINK = 'cf-popup-content-location-link'
export const CF_POPUP_CONTENT_LOCATION_NAME = 'cf-popup-content-location-name'
export const CF_POPUP_CONTENT_TAG = 'cf-popup-tag'
export const CF_POPUP_CONTENT_TEMPLATE = 'cf-popup-content-template'
export const CF_SELECT_LOCATION_BUTTON_CLICKED =
  'cf-select-location-button-clicked'

// LOCATIONS LIST
export const CF_LOCATION_CHECKBOX = 'cf-location-checkbox'
export const CF_LOCATION_CHECKBOX_INPUT = 'cf-location-checkbox-input'
export const CF_LOCATIONS_LIST_CONTAINER = 'cf-locations-list'
export const CF_MAP_QUESTION_ALERT_HIDDEN = 'cf-map-question-tag-alert-hidden'
export const CF_MAP_QUESTION_TAG_ALERT = 'cf-map-question-tag-alert'
export const CF_NO_SELECTIONS_MESSAGE = 'cf-no-selections-message'
export const CF_SELECTIONS_MESSAGE = 'cf-selections-message'
export const CF_SELECTED_LOCATION_MESSAGE = 'cf-selected-locations-message'
export const CF_SELECTED_LOCATIONS_CONTAINER = 'cf-selected-locations-container'

// FILTERS
export const CF_APPLY_FILTERS_BUTTON = 'cf-apply-filters-button'
export const CF_FILTER_HIDDEN = 'cf-filter-hidden'
export const CF_LOCATION_COUNT = 'cf-location-count'
export const CF_RESET_FILTERS_BUTTON = 'cf-reset-filters-button'

// PAGINATION
export const CF_PAGINATION_HIDDEN = 'cf-pagination-hidden'

// VIEW TOGGLE
export const CF_TOGGLE_HIDDEN = 'cf-toggle-hidden'
export const CF_SWITCH_TO_LIST_VIEW_BUTTON = 'cf-switch-to-list-view-button'
export const CF_SWITCH_TO_MAP_VIEW_BUTTON = 'cf-switch-to-map-view-button'

// MAP DEFAULTS
export const DEFAULT_LOCATION_ICON = 'locationMarkerIcon'
export const DEFAULT_MAP_CENTER_POINT: LngLatLike = [-122.3321, 47.6062]
export const DEFAULT_MAP_MARKER_TYPE = 'symbol'
export const DEFAULT_MAP_ZOOM = 8
export const LOCATIONS_LAYER = 'locations-layer'
export const LOCATIONS_SOURCE = 'locations'
export const SELECTED_LOCATION_ICON = 'locationMarkerIconSelected'
export const POPUP_LAYER = 'open-popup'

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

// Track the number of selected locations for each map
export const selectionCounts = new Map<string, number>()

// Query elements within a specific map container
export const mapQuerySelector = (
  mapId: string,
  className: string,
): Element | null => {
  return document.querySelector(`[${DATA_MAP_ID}="${mapId}"].${className}`)
}

// Get the internalized string and replace parameters like {0}, {1}, etc.
// TODO: implement a more robust solution for string internationalization in typescript
export const localizeString = (message: string, params: string[] = []) => {
  for (let i = 0; i < params.length; i++) {
    const placeholder = `{${i}}`
    message = message.replace(placeholder, params[i])
  }

  return message
}

export const queryLocationCheckboxes = (
  mapId: string,
): NodeListOf<HTMLElement> => {
  const locationsListContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement

  return locationsListContainer.querySelectorAll(`.${CF_LOCATION_CHECKBOX}`)
}

export const getVisibleCheckboxes = (mapId: string) => {
  const locationCheckboxes = queryLocationCheckboxes(mapId)
  return Array.from(locationCheckboxes).filter((checkbox) => {
    const checkboxElement = checkbox || null
    return (
      checkboxElement && !checkboxElement.classList.contains(CF_FILTER_HIDDEN)
    )
  })
}

export const getMessages = (): MapMessages => {
  return window.app?.data?.messages as MapMessages
}

export const hasReachedMaxSelections = (mapId: string): boolean => {
  const mapData = window.app?.data?.maps?.[mapId] as MapData

  if (!mapData.settings.maxLocationSelections) {
    return false
  }

  const maxLocationSelections = Number(mapData.settings.maxLocationSelections)

  const selectionCount = selectionCounts.get(mapId) || 0
  return selectionCount >= maxLocationSelections
}
