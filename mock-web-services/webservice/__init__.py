class WebService:

    def __init__(self, app):
        self.app = app

    def readFile(self, filename):
        with open(filename, "r") as file:
            jsonstr = file.read()

        return jsonstr

    def returnJsonResponse(self, jsonstr):
        return self.app.response_class(
            response=jsonstr, mimetype="application/json")

    def returnJsonResponseFromFile(self, filename):
        jsonstr = self.readFile(filename)
        return self.returnJsonResponse(jsonstr)
