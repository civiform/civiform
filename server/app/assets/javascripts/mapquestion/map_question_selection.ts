import {
  CF_LOCATIONS_LIST_CONTAINER,
  CF_NO_SELECTIONS_MESSAGE,
  CF_SELECTED_LOCATIONS_LIST,
  CF_SELECT_LOCATION_BUTTON,
  CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE,
  CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_INPUT,
  CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LABEL,
  mapQuerySelector,
} from './map_util'

export const initLocationSelection = (mapId: string): void => {
  const locationsListContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  )

  if (!locationsListContainer) return

  const locationCheckboxes = locationsListContainer.querySelectorAll(
    'input[type="checkbox"]',
  )
  locationCheckboxes.forEach((locationCheckbox) => {
    // Add event listener to each checkbox to update selected locations on change event
    locationCheckbox.addEventListener('change', () =>
      updateSelectedLocations(mapId),
    )
  })

  // Add event listener for select location buttons within this map's container
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement
    if (target.classList.contains(CF_SELECT_LOCATION_BUTTON)) {
      // TODO: Use location ID
      const locationName = target.getAttribute('data-location-name')
      if (locationName) {
        selectLocationFromMap(locationName, mapId)
        // Close any open popups on click of select location button
        // Change color of button when selected? But not close popup?
        const popups = document.querySelectorAll('.maplibregl-popup')
        popups.forEach((popup) => popup.remove())
      }
    }
  })
}

const updateSelectedLocations = (mapId: string): void => {
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  )

  if (!mapLocationsContainer) return

  const selectedCheckboxes = mapLocationsContainer.querySelectorAll(
    'input[type="checkbox"]:checked',
  )
  const noSelectionsMessage = mapQuerySelector(
    mapId,
    CF_NO_SELECTIONS_MESSAGE,
  ) as HTMLElement | null
  const selectedLocationsList = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATIONS_LIST,
  ) as HTMLElement | null

  if (!noSelectionsMessage || !selectedLocationsList) return

  if (selectedCheckboxes.length === 0) {
    // Show "no selections" message
    noSelectionsMessage.classList.remove('display-none')
    selectedLocationsList.classList.add('display-none')
  } else {
    // Hide "no selections" message and show selected locations
    noSelectionsMessage.classList.add('display-none')
    selectedLocationsList.classList.remove('display-none')

    // Clear and rebuild the selected locations list
    selectedLocationsList.innerHTML = ''

    selectedCheckboxes.forEach((checkbox) => {
      const locationLabel = mapLocationsContainer.querySelector(
        `label[for="${checkbox.id}"]`,
      )
      if (locationLabel) {
        const selectedItem = createSelectedLocationCheckboxFromTemplate(
          checkbox,
          locationLabel,
          mapId,
        )
        selectedLocationsList.appendChild(selectedItem)
      }
    })
  }
}

const selectLocationFromMap = (locationName: string, mapId: string): void => {
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  )
  if (!mapLocationsContainer) return

  // Find the checkbox with the matching location name within this map's container
  const checkboxes = mapLocationsContainer.querySelectorAll(
    'input[type="checkbox"]',
  )
  checkboxes.forEach((checkbox) => {
    const checkboxValue = checkbox.getAttribute('value')
    if (checkboxValue === locationName) {
      ;(checkbox as HTMLInputElement).checked = true
      // Trigger the change event to update the selected locations
      checkbox.dispatchEvent(new Event('change'))
    }
  })
}

const copyFieldContent = (
  fieldName: string,
  locationLabel: Element,
  selectedLocation: HTMLElement,
): void => {
  const sourceSelector = `.cf-location-checkbox-${fieldName}`
  const templateSelector = `.cf-selected-location-checkbox-template-${fieldName}`

  const sourceElement = locationLabel.querySelector(sourceSelector)
  const templateElement = selectedLocation.querySelector(
    templateSelector,
  ) as HTMLElement

  if (sourceElement && templateElement) {
    // For links, copy href attribute
    if (fieldName === 'link') {
      const sourceLink = sourceElement as HTMLAnchorElement
      const templateLink = templateElement as HTMLAnchorElement
      templateLink.href = sourceLink.href
    } else {
      templateElement.textContent = sourceElement.textContent
    }
  } else if (templateElement) {
    templateElement.style.display = 'none'
  }
}

const createSelectedLocationCheckboxFromTemplate = (
  originalCheckbox: Element,
  locationLabel: Element,
  mapId: string,
): HTMLElement => {
  const selectedLocationTemplate = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE,
  )
  if (!selectedLocationTemplate) {
    throw new Error(`Selected location template not found for map ${mapId}`)
  }

  const selectedLocation = selectedLocationTemplate.cloneNode(
    true,
  ) as HTMLElement
  selectedLocation.classList.remove(
    'hidden',
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE,
  )

  // Set up checkbox and label IDs
  const selectedCheckbox = selectedLocation.querySelector(
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_INPUT,
  ) as HTMLInputElement
  const selectedLabel = selectedLocation.querySelector(
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LABEL,
  ) as HTMLLabelElement
  const selectedCheckboxId = `selected-${originalCheckbox.id}`
  selectedCheckbox.id = selectedCheckboxId
  selectedLabel.htmlFor = selectedCheckboxId

  // Copy content from source checkbox to selected checkbox template
  copyFieldContent('name', locationLabel, selectedLocation)
  copyFieldContent('address', locationLabel, selectedLocation)
  copyFieldContent('link', locationLabel, selectedLocation)

  // Add event listener for unchecking original checkbox
  selectedCheckbox.addEventListener('change', () => {
    const originalElement = document.getElementById(
      originalCheckbox.id,
    ) as HTMLInputElement
    if (originalElement) {
      originalElement.checked = false
      updateSelectedLocations(mapId)
    }
  })

  return selectedLocation
}
