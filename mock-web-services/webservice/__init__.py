class WebService:
    """Base class contain helper methods for returning responses."""

    def __init__(self, app):
        self.app = app

    def read_file(self, file_path):
        """Reads a file from path and returns it as a string."""
        with open(file_path, "r") as file:
            jsonstr = file.read()

        return jsonstr

    def return_json_response(self, jsonstr):
        """Return a string of json as the response as the correct mimetype."""
        return self.app.response_class(response=jsonstr, mimetype="application/json")

    def return_json_response_from_file(self, file_path):
        """Reads a file from path and returns a response formatted as json"""
        jsonstr = self.read_file(file_path)
        return self.return_json_response(jsonstr)
