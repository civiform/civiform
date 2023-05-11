# Mock Web Services

The Mock Web Services is a lightweight Python Flask app that is used to create
mock REST endpoints to mimic external vendor APIs. This allows for full testing,
including the network, of integrations to these APIs without being bound to the
real implementations.

## Running

To start this server run `python app.py` from this directory or start the docker
container `docker run --name mockweb -p 8000:8000 civiform/mock-web-services:latest`

## Esri Endpoints

There are currently two endpoints to handle Esri ArcGIS endpoints we leverage.
One for finding address candidates and one for determining service area
features.

These endpoints return JSON from files shared with the FakeEsriClient.java and
unit tests.

### Find Address Candidates

Endpoint to mock Esri [findAddressCandidates](https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm). We use the address as a key in determining which type of response this will return.

Valid options are:

- Address In Area
- Legit Address
- Bogus Address

#### Examples in curl

- `curl http://localhost:8000/esri/findAddressCandidates?address=Address%20In%20Area`
- `curl http://localhost:8000/esri/findAddressCandidates?address=Legit%20Address`
- `curl http://localhost:8000/esri/findAddressCandidates?address=Bogus%20Address`

### Service Area Features with Map Service/Layer

Endpoint to mock Esri [Map Service/Layer](https://developers.arcgis.com/rest/services-reference/enterprise/query-feature-service-layer-.htm) and used for service area validations. We use the _y_ value in the geometry json parameter as a key in determining which type of response this will return.

Valid options are:

- 100.0
- 101.0
- 102.0

#### Examples in curl

- `curl http://localhost:8000/esri/serviceAreaFeatures?geometry=%7B'x'%3A-100.0,'y'%3A100.0,'spatialReference'%3A4326%7D`
- `curl http://localhost:8000/esri/serviceAreaFeatures?geometry=%7B'x'%3A-100.0,'y'%3A101.0,'spatialReference'%3A4326%7D`
- `curl http://localhost:8000/esri/serviceAreaFeatures?geometry=%7B'x'%3A-100.0,'y'%3A102.0,'spatialReference'%3A4326%7D`
