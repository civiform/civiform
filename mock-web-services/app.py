from flask import Flask, Response, request, json
from esri import EsriMockWebService

app = Flask(__name__)
esri_ws = EsriMockWebService(app)


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
        print(e)
        return Response("Bad request. " + e, status=400)


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
        print(e)
        return Response("Bad request. " + e, status=400)


if __name__ == "__main__":
    app.debug = True
    app.run(host="0.0.0.0", port=8000)
