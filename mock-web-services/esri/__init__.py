from webservice import WebService


class EsriMockWebService(WebService):
    """
    Mock service for Esri endpoints that can map specific key input values to
    pre-created json stored in files.

    The files used by this service are shared with the FakeEsriClient.java class
    and used in the unit tests.
    """

    def __init__(self, app):
        WebService.__init__(self, app)
        self.app = app
        self.file_root = "../server/test/resources/esri/"

    def find_address_candidates(self, address):
        """
        Returns json response for Esri's findAddressCandidates endpoint
        based on key in put address value.
        """
        if address == "Address In Area" or address == "Legit Address":
            return self.return_json_response_from_file(
                self.file_root + "findAddressCandidates.json")
        elif address == "Bogus Address":
            return self.return_json_response_from_file(
                self.file_root + "findAddressCandidatesNoCandidates.json")
        elif address == "Empty Response":
            return self.return_json_response_from_file(
                self.file_root + "findAddressCandidatesEmptyResponse.json")
        elif address == "Esri Error Response":
            return self.return_json_response_from_file(
                self.file_root + "esriErrorResponse.json")
        else:
            raise Exception("Invalid mock request")

    def service_area_features(self, latitude):
        """
        Returns json response for Esri's service area endpoint based on key
        input value of the latitude
        """
        if latitude == 100.0:
            return self.return_json_response_from_file(
                self.file_root + "serviceAreaFeatures.json")
        elif latitude == 101.0:
            return self.return_json_response_from_file(
                self.file_root + "serviceAreaFeaturesNoFeatures.json")
        elif latitude == 102.0:
            return self.return_json_response_from_file(
                self.file_root + "serviceAreaFeaturesNotInArea.json")
        else:
            raise Exception("Invalid mock request")
