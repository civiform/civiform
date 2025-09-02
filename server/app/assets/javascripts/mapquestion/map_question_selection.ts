import {
  CF_LOCATIONS_LIST_CONTAINER,
  CF_LOCATION_CHECKBOX,
  CF_SELECTED_LOCATIONS_CONTAINER,
  CF_NO_SELECTIONS_MESSAGE,
  mapQuerySelector,
  DATA_FEATURE_ID_ATTR,
  DATA_MAP_ID_ATTR,
  CF_LOCATION_CHECKBOX_INPUT,
} from './map_util'


export const initLocationSelection = (mapId: string): void => {
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (!mapLocationsContainer) return

  const locationInputs = mapLocationsContainer.querySelectorAll(
    `.${CF_LOCATION_CHECKBOX} input[type="checkbox"]`,
  )


  // Initial update so the previously saved locations get displayed as selected
  updateSelectedLocations(mapId)
}


export const selectLocationsFromMap = (featureId: string, mapId: string): void => {
  const locationsListContainer = mapQuerySelector(mapId, CF_LOCATIONS_LIST_CONTAINER) as HTMLElement | null
  if (!locationsListContainer) return

  const targetCheckbox = locationsListContainer.querySelector(`[${DATA_FEATURE_ID_ATTR}="${featureId}"]`)
  if (targetCheckbox) {
    const checkboxInputElement = targetCheckbox.querySelector(`.${CF_LOCATION_CHECKBOX_INPUT}`) as HTMLInputElement
    if (checkboxInputElement) {
      checkboxInputElement.checked = true
      updateSelectedLocations(mapId)
    }
  }
}

export const updateSelectedLocations = (mapId: string): void => {
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  )

  if (!mapLocationsContainer) {
    return
  }

  const selectedCheckboxes = mapLocationsContainer.querySelectorAll(
    `.${CF_LOCATION_CHECKBOX}:has(input[type="checkbox"]:checked)`,
  )

  const selectedLocationsContainer = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATIONS_CONTAINER,
  ) as HTMLElement | null

  const noSelectionsTemplate = mapQuerySelector(
    mapId,
    CF_NO_SELECTIONS_MESSAGE,
  ) as HTMLTemplateElement | null

  if (!selectedLocationsContainer || !noSelectionsTemplate) {
    return
  }
  if (selectedCheckboxes.length === 0) {
    selectedLocationsContainer.textContent = ''
    const noSelectionsElement = noSelectionsTemplate.content.cloneNode(true)
    selectedLocationsContainer.appendChild(noSelectionsElement)
  } else {
    selectedLocationsContainer.textContent = ''
    selectedCheckboxes.forEach((originalCheckbox) => {
      const selectedLocation = originalCheckbox.cloneNode(true) as HTMLElement

      const input = selectedLocation.querySelector(
        'input[type="checkbox"]',
      ) as HTMLInputElement
      const label = selectedLocation.querySelector('label') as HTMLLabelElement

      if (input && label) {
        const originalId = input.id
        const selectedId = `selected-${originalId}`
        const featureId = originalCheckbox.getAttribute(DATA_FEATURE_ID_ATTR)
        
        input.id = selectedId
        label.htmlFor = selectedId
        input.setAttribute(DATA_MAP_ID_ATTR, mapId)
        
        if (featureId) {
          input.setAttribute(DATA_FEATURE_ID_ATTR, featureId)
        }
      }

      selectedLocationsContainer.appendChild(selectedLocation)
    })
  }
}
