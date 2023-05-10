from flask import Flask, Response, request, json
from esri import EsriMockWebService

app = Flask(__name__)
esriWS = EsriMockWebService(app)


@app.route("/esri/findAddressCandidates")
def findAddressCandidates():
    try:
        address = request.args.get("address")
        return esriWS.findAddressCandidates(address)
    except Exception as e:
        print(e)
        return Response("Bad request", status=400)


@app.route("/esri/serviceAreaFeatures")
def serviceAreaFeatures():
    try:
        geometry = json.loads(request.args.get("geometry").replace("'", '"'))
        latitude = geometry.get("y")
        return esriWS.serviceAreaFeatures(latitude)
    except Exception as e:
        print(e)
        return Response("Bad request", status=400)


if __name__ == "__main__":
    app.debug = True
    app.run(host="0.0.0.0", port=8000)
