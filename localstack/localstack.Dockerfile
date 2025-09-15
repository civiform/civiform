# syntax=docker/dockerfile:1@sha256:dabfc0969b935b2080555ace70ee69a5261af8a8f1b4df97b9e7fbcf6722eddf
FROM localstack/localstack:4.8.0@sha256:fe24345d30a3cdc3cead7d88ebbdf39ae480fa3fe4de1765b98a9a066b8515cf

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
