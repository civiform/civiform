from flask import Blueprint, Response, json, request
"""
Mock service for Esri endpoints that can map specific key input values to
pre-created json stored in files.

The files used by this service are shared with the FakeEsriClient.java class
and used in the unit tests.
"""

esri_blueprint = Blueprint("esri", __name__)
file_root = "../server/test/resources/esri/"


def return_json_response_from_file(file_name) -> Response:
    """Reads a file from path and returns a response formatted as json"""
    with open(file_root + file_name, "r") as file:
        jsonstr = file.read()

    print(jsonstr, flush=True)
    """Return a string of json as the response as the correct mimetype."""
    response = Response(
        response=jsonstr, mimetype="application/json", status=200)
    return response


@esri_blueprint.route("/findAddressCandidates")
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
        """
        Returns json response for Esri's findAddressCandidates endpoint
        based on key in put address value.
        """
        if address == "Address In Area" or address == "Legit Address":
            return return_json_response_from_file("findAddressCandidates.json")
        elif address == "Bogus Address":
            return return_json_response_from_file(
                "findAddressCandidatesNoCandidates.json")
        elif address == "Empty Response":
            return return_json_response_from_file(
                "findAddressCandidatesEmptyResponse.json")
        elif address == "Esri Error Response":
            return return_json_response_from_file("esriErrorResponse.json")
        else:
            raise Exception("Invalid mock request")
    except Exception as e:
        print(e, flush=True)
        return Response("Bad request.", status=400)


@esri_blueprint.route("/serviceAreaFeatures")
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
        """
        Returns json response for Esri's service area endpoint based on key
        input value of the latitude
        """
        if latitude == 100.0:
            return return_json_response_from_file("serviceAreaFeatures.json")
        elif latitude == 101.0:
            return return_json_response_from_file(
                "serviceAreaFeaturesNoFeatures.json")
        elif latitude == 102.0:
            return return_json_response_from_file(
                "serviceAreaFeaturesNotInArea.json")
        else:
            raise Exception("Invalid mock request")
    except Exception as e:
        print(e, flush=True)
        return Response("Bad request.", status=400)
