import {Map, Marker, Popup} from 'maplibre-gl'
import {FeatureCollection} from 'geojson'

const testData: FeatureCollection = {
  type: 'FeatureCollection',
  features: [
    {
      type: 'Feature',
      properties: {
        name: 'Little Explorers Preschool',
        address: '123 Maple St, Seattle, WA 98101',
      },
      geometry: {
        type: 'Point',
        coordinates: [-122.3421, 47.6101],
      },
    },
    {
      type: 'Feature',
      properties: {
        name: 'Bright Beginnings Academy',
        address: '456 Pine St, Seattle, WA 98101',
      },
      geometry: {
        type: 'Point',
        coordinates: [-122.335, 47.6119],
      },
    },
    {
      type: 'Feature',
      properties: {
        name: 'Rainier Kids Center',
        address: '789 Rainier Ave S, Seattle, WA 98144',
        open_late: true,
      },
      geometry: {
        type: 'Point',
        coordinates: [-122.308, 47.5902],
      },
    },
    {
      type: 'Feature',
      properties: {
        name: 'Greenwood Daycare',
        address: '101 Greenwood Ave N, Seattle, WA 98103',
        open_late: true,
      },
      geometry: {
        type: 'Point',
        coordinates: [-122.355, 47.6941],
      },
    },
    {
      type: 'Feature',
      properties: {
        name: 'Capitol Hill Child Care',
        address: '202 Broadway E, Seattle, WA 98102',
      },
      geometry: {
        type: 'Point',
        coordinates: [-122.3208, 47.6215],
      },
    },
  ],
}

export const init = () => {
  populateMap()
}

export const addLocationToApplicantList = (name: string) => {
  console.log('Location added to applicant list:', name)
}

export const populateMap = () => {
  const map = new Map({
    container: 'map', // container id
    style: {
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
    },
    center: [-122.3321, 47.6062], // starting position [lng, lat]
    zoom: 8, // starting zoom
  })

  testData.features.forEach((feature) => {
    const popupDomElement = document.createElement('div')
    const addToMyListButton = document.createElement('div')
    addToMyListButton.innerHTML = `<button type="button" class="usa-button usa-button--outline">Add to my list</button>`
    popupDomElement.textContent = feature.properties?.name as string
    popupDomElement.appendChild(addToMyListButton)
    addToMyListButton.addEventListener('click', () => {
      addLocationToApplicantList(feature.properties?.name as string)
    })

    if (feature.geometry.type === 'Point') {
      const popupElement: Popup = new Popup({closeButton: false}).setDOMContent(
        popupDomElement,
      )
      new Marker()
        .setLngLat([
          feature.geometry.coordinates[0],
          feature.geometry.coordinates[1],
        ])
        .setPopup(popupElement)
        .addTo(map)
    }
  })
}
