import {
  CF_LOCATIONS_LIST_CONTAINER,
  CF_LOCATION_CHECKBOX,
  CF_SELECTED_LOCATIONS_CONTAINER,
  CF_NO_SELECTIONS_MESSAGE,
  mapQuerySelector,
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

  locationInputs.forEach((input) => {
    // Add event listener to each checkbox input to update selected locations on change event
    input.addEventListener('change', () => {
      updateSelectedLocations(mapId)
    })
  })

  // Initial update so the previously saved locations get displayed as selected
  updateSelectedLocations(mapId)
}

const updateSelectedLocations = (mapId: string): void => {
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
        input.id = selectedId
        label.htmlFor = selectedId

        input.addEventListener('change', () => {
          if (!input.checked) {
            const originalInput = document.getElementById(
              originalId,
            ) as HTMLInputElement
            if (originalInput) {
              originalInput.checked = false
              updateSelectedLocations(mapId)
            }
          }
        })
      }

      selectedLocationsContainer.appendChild(selectedLocation)
    })
  }
}
