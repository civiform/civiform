# syntax=docker/dockerfile:1@sha256:9857836c9ee4268391bb5b09f9f157f3c91bb15821bb77969642813b0d00518d
FROM python:3.13.3-slim@sha256:21e39cf1815802d4c6f89a0d3a166cc67ce58f95b6d1639e68a394c99310d2e5

RUN useradd --create-home appuser --no-log-init

USER appuser

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt --no-warn-script-location
COPY . .
COPY --from=server /test/resources/esri /server/test/resources/esri

EXPOSE 8000

CMD ["python", "app.py"]

