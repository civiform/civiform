from esri import esri_blueprint
from flask import Flask

app = Flask(__name__)
app.register_blueprint(esri_blueprint, url_prefix="/esri")

if __name__ == "__main__":
    app.debug = True
    app.run(host="0.0.0.0", port=8000)
