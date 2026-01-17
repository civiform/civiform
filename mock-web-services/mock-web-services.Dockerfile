# syntax=docker/dockerfile:1@sha256:b6afd42430b15f2d2a4c5a02b919e98a525b785b1aaff16747d2f623364e39b6
FROM python:3.14.2-slim@sha256:9b81fe9acff79e61affb44aaf3b6ff234392e8ca477cb86c9f7fd11732ce9b6a

RUN useradd --create-home appuser --no-log-init

USER appuser

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt --no-warn-script-location
COPY . .
COPY --from=server /test/resources/esri /server/test/resources/esri
COPY --from=server /test/resources/geojson /server/test/resources/geojson

EXPOSE 8000

CMD ["python", "app.py"]

