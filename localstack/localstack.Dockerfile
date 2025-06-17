# syntax=docker/dockerfile:1@sha256:e63addfe27b10e394a5f9f1e866961adc70d08573f1cb80f8d1a0999347b3553
FROM localstack/localstack:4.5.0@sha256:9d4253786e0effe974d77fe3c390358391a56090a4fff83b4600d8a64404d95d

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
