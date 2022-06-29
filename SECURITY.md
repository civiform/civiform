# Security Policy

## Supported Versions

Every merge into `main` triggers a build and pushes a server image to [CiviForm Docker Hub](https://hub.docker.com/repository/docker/civiform/civiform) with a unique snapshot for the build.

At the moment, all production deployments are encouraged to deploy at least weekly and keep in regular communication with the development team.

In Q2 2022 we are planning to introduce [SemVer](https://semver.org/) builds with official releases of the server image.

## Reporting a Vulnerability

To report a vulnerability, please email civiform-escalations@googlegroups.com

Please include the docker image tag for the version in which you have found the vulnerability, or a link to code on GitHub if that is more appropriate.
