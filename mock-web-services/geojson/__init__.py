from flask import Blueprint, Response, json, request
"""
Mock service for GeoJSON endpoints that can map specific key input values to
pre-created GeoJSON stored in files.
"""

geojson_blueprint = Blueprint("geojson", __name__)
file_root = "../server/test/resources/geojson/"


def return_json_response_from_file(file_name) -> Response:
    """Reads a file from path and returns a response formatted as json"""
    with open(file_root + file_name, "r") as file:
        jsonstr = file.read()

    print(jsonstr, flush=True)
    """Return a string of json as the response as the correct mimetype."""
    response = Response(
        response=jsonstr, mimetype="application/json", status=200)
    return response


@geojson_blueprint.route("/data")
def get_geojson_data():
    """
    Endpoint to serve sample GeoJSON data for testing.
    Returns the sample locations GeoJSON file.
    """
    try:
        return return_json_response_from_file("sample_locations.json")
    except Exception as e:
        print(e, flush=True)
        return Response("Error loading GeoJSON data.", status=500)


