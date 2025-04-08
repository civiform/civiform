# syntax=docker/dockerfile:1@sha256:4c68376a702446fc3c79af22de146a148bc3367e73c25a5803d453b6b3f722fb
FROM python:3.13.2-slim@sha256:6b3223eb4d93718828223966ad316909c39813dee3ee9395204940500792b740

RUN useradd --create-home appuser --no-log-init

USER appuser

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt --no-warn-script-location
COPY . .
COPY --from=server /test/resources/esri /server/test/resources/esri

EXPOSE 8000

CMD ["python", "app.py"]

