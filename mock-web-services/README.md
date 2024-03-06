# Mock Web Services

The Mock Web Services is a lightweight Python Flask app that is used to create
mock REST endpoints to mimic external vendor APIs. This allows for full testing,
including the network, of integrations to these APIs without being bound to the
real implementations.

## Running

To start this server run `python app.py` from this directory or start the docker
container `docker run --name mockweb -p 8000:8000 civiform/mock-web-services:latest`

### Testing changes locally

If you want to modify Mock Web Services and verify your changes locally, there
are two options.

#### Option 1: Python

You can use the `app.py` script to run the app locally:

```bash
cd mock-web-services
pip install -r requirements.txt
python app.py
```

Then, you can check the responses on http://localhost:8000 (see sample URLs
below). This is the fastest way to iterate.

#### Option 2: Local Server

You can also re-build the Mock Web Services then re-run your local server to see
the changes in the context of the CiviForm app:

```bash
bin/build-mock-web-services
bin/run-dev
```

If it doesn't seem to update, try deleting the `mock-web-services` container
under `civiform` and try again.

## Esri Endpoints

There are currently two endpoints to handle Esri ArcGIS endpoints we leverage.
One for finding address candidates and one for determining service area
features.

These endpoints return JSON from files shared with the FakeEsriClient.java and
unit tests.

## Determining

The EsriModule is used to determine if we load the RealEsriClient or the FakeEsriClient.
When using the mock service we want to use the RealEsriClient. This is what is
used when a value is provided for "esri_find_address_candidates_url". If no
value is configured we fall back to the FakeEsriClient.

### Find Address Candidates

Endpoint to mock Esri [findAddressCandidates](https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm). We use the address as a key in determining which type of response this will return.

Valid options are:

- Address In Area
- Legit Address
- Bogus Address

See [find_address_candidates](https://github.com/civiform/civiform/blob/ad287486d941812ecbcd6d51926b35f14b1c531c/mock-web-services/esri/__init__.py#L18) in `esri/__init__.py`.

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

See [#service_area_features](https://github.com/civiform/civiform/blob/ad287486d941812ecbcd6d51926b35f14b1c531c/mock-web-services/esri/__init__.py#L32) in `esri/__init__.py`.

Note that those `y` values correlate to address suggestions from
`server/test/resources/esri/findAddressCandidates.json`:

- "Address In Area" has `y = 100.0`
- "Address With No Service Area Features" has `y = 101.0`
- "Address Not In Area" has `y = 102.0`

#### Examples in curl

- `curl http://localhost:8000/esri/serviceAreaFeatures?geometry=%7B'x'%3A-100.0,'y'%3A100.0,'spatialReference'%3A4326%7D`
- `curl http://localhost:8000/esri/serviceAreaFeatures?geometry=%7B'x'%3A-100.0,'y'%3A101.0,'spatialReference'%3A4326%7D`
- `curl http://localhost:8000/esri/serviceAreaFeatures?geometry=%7B'x'%3A-100.0,'y'%3A102.0,'spatialReference'%3A4326%7D`
