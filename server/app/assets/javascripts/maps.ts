// Javascript handling for maps

import L, { FeatureGroup, LayerGroup } from 'leaflet'
import {assertNotNull} from './util'
import { FeatureCollection, GeoJsonGeometryTypes, GeoJsonObject, GeoJsonTypes } from 'geojson'

let selectedProviderNames: Array<string> = []

let onlyOpenLate: boolean;

export function init() {
  const checkbox = document.getElementById('open_late');
  const map = L.map('map').setView([47.6062, -122.3321], 12) // Seattle coords

  checkbox?.addEventListener('change', (e: Event) => {
    const target = <HTMLInputElement>e.target
    onlyOpenLate = target.checked
    renderMarkers(layerGroup, providersGeoJson)
  })

  selectedProviderNames = []
  const providersGeoJson = JSON.parse(
    document.getElementById('all-providers')?.getAttribute('value') as string,
  )
  L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
      attribution: '&copy; Esri'
    }
  ).addTo(map)

  const layerGroup = new LayerGroup().addTo(map);

  renderMarkers(layerGroup, providersGeoJson);
}

const renderMarkers = (markerGroup: LayerGroup, providers: FeatureCollection<any>) => {
  markerGroup.clearLayers()
  L.geoJSON(filterProviders(providers),
    {
      onEachFeature: function (feature, layer) {
        const popup = L.DomUtil.create('div', 'infoWindow')
        popup.innerHTML = `<strong>${feature.properties.name}</strong><br>${feature.properties.address}<br><a class="usa-button-outline" href="www.google.com" target="_blank">See more</a><br><button id="add-to-my-list" class="add-to-my-list usa-button">Add to my list</button>`
        layer.bindPopup(popup).on('popupopen', (a) => {
          const popUp = a.target.getPopup()
          popUp
            .getElement()
            .querySelector('#add-to-my-list')
            .addEventListener('click', () => {
              addLocationToMyList(feature.properties.name, feature.properties.address)
            })
        })
      }
    }
  ).addTo(markerGroup);
}

export function addLocationToMyList(providerName: string, providerAddress: string) {
  if (selectedProviderNames.includes(providerName)) {
    // do nothing
    console.log(`${providerName} has already been added!`)
  } else {
    // Copy the selected provider template
    const newField = assertNotNull(
      document.getElementById('selected-provider-template'),
    ).cloneNode(true) as HTMLElement
    newField.classList.remove('hidden')
    newField.removeAttribute('id')
    newField.innerHTML=`<td>${providerName}</td><td>${providerAddress}</td>`

    // Add to the end of selected providers table.
    const selectedProviders = assertNotNull(
      document.getElementById('selected-providers'),
    )

    selectedProviders.appendChild(newField)
    selectedProviderNames.push(providerName)
  }
}

const filterProviders = (providers: FeatureCollection<any>) => {
  if (onlyOpenLate) {
    const filteredProviders: FeatureCollection = {
      type: 'FeatureCollection',
      features: providers.features.filter(item => {
        return item.properties?.open_late === true;
      })
    };
    return filteredProviders;
  } else {
    return providers;
  }
}