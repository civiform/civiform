# syntax=docker/dockerfile:1@sha256:38387523653efa0039f8e1c89bb74a30504e76ee9f565e25c9a09841f9427b05
FROM localstack/localstack:4.7.0@sha256:12253acd9676770e9bd31cbfcf17c5ca6fd7fb5c0c62f3c46dd701f20304260c

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
