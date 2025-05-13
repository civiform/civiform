from apibridge import ApiBridgeMockWebService
from esri import EsriMockWebService
from flask import Flask, Response, json, request

app = Flask(__name__)
esri_ws = EsriMockWebService(app)
apibridge_ws = ApiBridgeMockWebService(app)


@app.route("/esri/findAddressCandidates")
def find_address_candidates():
    """
    Endpoint to mock Esri findAddressCandidates. We use the address as a key in
    determining which type of response this will return.

    Valid options are:
        * Address In Area
        * Legit Address
        * Bogus Address
    """
    try:
        address = request.args.get("address")
        return esri_ws.find_address_candidates(address)
    except Exception as e:
        print(e, flush=True)
        return Response("Bad request.", status=400)


@app.route("/esri/serviceAreaFeatures")
def service_area_features():
    """
    Endpoint to mock Esri service area validation. We use the latitude as a
    key in determining which type of response this will return.

    Valid options are:
        * 100.0
        * 101.0
        * 102.0
    """
    try:
        geometry = json.loads(request.args.get("geometry").replace("'", '"'))
        latitude = geometry.get("y")
        return esri_ws.service_area_features(latitude)
    except Exception as e:
        print(e, flush=True)
        return Response("Bad request.", status=400)


@app.route("/api-bridge/health-check")
def apibridge_healthcheck():
    """API Bridge Healthcheck Endpoint"""
    try:
        return apibridge_ws.healthcheck()
    except Exception as e:
        print(e, flush=True)
        return Response("Bad request.", status=500)


@app.route("/api-bridge/discovery")
def apibridge_discovery():
    """API Bridge Discovery Endpoint"""
    try:
        return apibridge_ws.discovery()
    except Exception as e:
        print(e, flush=True)
        return Response("Bad request.", status=500)


@app.route("/api-bridge/bridge/<slug>", methods=["POST"])
def apibridge_bridge(slug: str):
    """API Bridge Variable Bridge Endpoint"""
    try:
        data = request.get_json()
        print(data, flush=True)
        return apibridge_ws.bridge(slug, data)
    except Exception as e:
        print(e, flush=True)
        return Response("Bad request.", status=500)


if __name__ == "__main__":
    app.debug = True
    app.run(host="0.0.0.0", port=8000)
