# syntax=docker/dockerfile:1@sha256:4c68376a702446fc3c79af22de146a148bc3367e73c25a5803d453b6b3f722fb
FROM localstack/localstack:4.3.0@sha256:f3cb1a79f3add997575e859c3a2808e6dae4b0de836661de255baa2b576868f8

# Localstack tries to connect to the host specified
# by success_redirect_url upon successful upload of
# content to S3. When running on a local machine,
# this will be "localhost". Within the localstack
# container, this doesn't point to the Civiform
# container. In order to allow this to resolve,
# nginx is used as a reverse proxy to forward
# requests localhost:9000 requests to civiform:9000
# (accessible from Docker's internal networking).
# See https://github.com/seattle-uat/civiform/issues/2639.
RUN apt-get update --assume-yes
RUN apt-get install nginx --assume-yes

COPY localstack.nginx.conf /etc/nginx/conf.d/
ADD localstack-docker-entrypoint.sh /usr/local/bin/

ENTRYPOINT ["localstack-docker-entrypoint.sh"]
