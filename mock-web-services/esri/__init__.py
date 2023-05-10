from webservice import WebService


class EsriMockWebService(WebService):

    def __init__(self, app):
        WebService.__init__(self, app)
        self.app = app
        self.file_root = "../server/test/resources/esri/"

    def findAddressCandidates(self, address):
        if address == "Address In Area" or address == "Legit Address":
            return self.returnJsonResponseFromFile(
                self.file_root + "findAddressCandidates.json")
        elif address == "Bogus Address":
            return self.returnJsonResponseFromFile(
                self.file_root + "findAddressCandidatesNoCandidates.json")
        else:
            raise Exception("Invalid mock request")

    def serviceAreaFeatures(self, latitude):
        if latitude == 100.0:
            return self.returnJsonResponseFromFile(
                self.file_root + "serviceAreaFeatures.json")
        elif latitude == 101.0:
            return self.returnJsonResponseFromFile(
                self.file_root + "serviceAreaFeaturesNoFeatures.json")
        elif latitude == 102.0:
            return self.returnJsonResponseFromFile(
                self.file_root + "serviceAreaFeaturesNotInArea.json")
        else:
            raise Exception("Invalid mock request")
