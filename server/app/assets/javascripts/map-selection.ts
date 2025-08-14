import {
  CF_LOCATIONS_LIST_CONTAINER,
  CF_NO_SELECTIONS_MESSAGE,
  CF_SELECTED_LOCATIONS_LIST,
  CF_SELECT_LOCATION_BUTTON,
  mapQuerySelector,
} from './map-util'

const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE = 'cf-selected-location-checkbox-template';
const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LABEL = '.cf-selected-location-checkbox-template-label';
const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_INPUT = '.cf-selected-location-checkbox-template-input';
const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_NAME = '.cf-selected-location-checkbox-template-name';
const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_ADDRESS = '.cf-selected-location-checkbox-template-address';
const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LINK_CONTAINER = '.cf-selected-location-checkbox-template-link-container';
const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LINK = '.cf-selected-location-checkbox-template-link';
const CF_LOCATION_CHECKBOX_NAME = '.cf-location-checkbox-name';
const CF_LOCATION_CHECKBOX_ADDRESS = '.cf-location-checkbox-address';
const CF_LOCATION_CHECKBOX_LINK = '.cf-location-checkbox-link';

export const initLocationSelection = (mapId: string) => {
  const locationsListContainer = mapQuerySelector(mapId, CF_LOCATIONS_LIST_CONTAINER)

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

export const updateSelectedLocations = (mapId: string) => {
  const mapLocationsContainer = mapQuerySelector(mapId, CF_LOCATIONS_LIST_CONTAINER)

  if (!mapLocationsContainer) return

  const selectedCheckboxes = mapLocationsContainer.querySelectorAll(
    'input[type="checkbox"]:checked',
  )
  const noSelectionsMessage = mapQuerySelector(mapId, CF_NO_SELECTIONS_MESSAGE)
  const selectedLocationsList = mapQuerySelector(mapId, CF_SELECTED_LOCATIONS_LIST)

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
        const selectedItem = createSelectedLocationFromTemplate(
          checkbox,
          locationLabel,
          mapId,
        )
        selectedLocationsList.appendChild(selectedItem)
      }
    })
  }
}

export const selectLocationFromMap = (locationName: string, mapId: string) => {
  const mapLocationsContainer = mapQuerySelector(mapId, CF_LOCATIONS_LIST_CONTAINER)
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

const createSelectedLocationFromTemplate = (
  originalCheckbox: Element,
  locationLabel: Element,
  mapId: string,
): HTMLElement => {
  const selectedLocationTemplate = mapQuerySelector(mapId, CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE);
  if (!selectedLocationTemplate) {
    throw new Error(`Selected location template not found for map ${mapId}`)
  }
  const selectedLocation = selectedLocationTemplate.cloneNode(true) as HTMLElement
  selectedLocation.classList.remove('hidden')
  selectedLocation.classList.remove(CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE)

  const selectedLocationCheckbox = selectedLocation.querySelector(
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_INPUT,
  ) as HTMLInputElement
  const selectedCheckboxId = `selected-${originalCheckbox.id}`
  selectedLocationCheckbox.id = selectedCheckboxId

  const selectedCheckboxLabel = selectedLocation.querySelector(CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LABEL) as HTMLLabelElement
  selectedCheckboxLabel.htmlFor = selectedCheckboxId

  const nameDiv = locationLabel.querySelector(CF_LOCATION_CHECKBOX_NAME)
  const templateName = selectedLocation.querySelector(CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_NAME)
  if (nameDiv && templateName) {
    templateName.textContent = nameDiv.textContent || ''
  }

  const addressDiv = locationLabel.querySelector(CF_LOCATION_CHECKBOX_ADDRESS)
  const templateAddress = selectedLocation.querySelector(CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_ADDRESS) as HTMLElement
  if (addressDiv && templateAddress) {
    templateAddress.textContent = addressDiv.textContent || ''
  } else if (templateAddress) {
    templateAddress.style.display = 'none'
  }

  const linkElement = locationLabel.querySelector(CF_LOCATION_CHECKBOX_LINK)
  const templateLink = selectedLocation.querySelector(CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LINK_CONTAINER) as HTMLElement
  if (linkElement && templateLink) {
    const templateAnchor = templateLink.querySelector(CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LINK) as HTMLAnchorElement
    console.log('Setting link for selected location:', templateAnchor);
    templateAnchor.href = linkElement.getAttribute('href') || ''
    templateAnchor.textContent = linkElement.textContent || ''
  } else if (templateLink) {
    templateLink.style.display = 'none'
  }

  // Add event listener for selected checkbox to uncheck original checkbox on change
  selectedLocationCheckbox.addEventListener('change', () => {
    const originalElement = document.getElementById(
      originalCheckbox.id,
    ) as HTMLInputElement
    if (originalElement) {
      originalElement.checked = false
      updateSelectedLocations(mapId)
    }
  })

  console.log('Created selected location element:', selectedLocation);
  return selectedLocation
}
