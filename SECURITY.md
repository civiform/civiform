# Security Policy

## Supported Versions

See our [documentation on release versioning](https://docs.civiform.us/it-manual/sre-playbook/upgrading-to-a-new-release).

We cut a release about every two weeks. Security fixes ship in the next scheduled release, so we recommend that all production deployments upgrade with each release and keep in regular communication with the development team. For a critical issue that affects all deployments, we may cut an out-of-schedule release.

## Reporting a Vulnerability

Please report vulnerabilities privately through GitHub's ["Report a vulnerability" form](https://github.com/civiform/civiform/security/advisories/new). This goes directly to the maintainers and lets us coordinate a fix with you before disclosure.

Please include the docker image tag for the version in which you have found the vulnerability, or a link to code on GitHub if that is more appropriate.

## What happens after you report

We review every report, and reports from governments that run CiviForm get acknowledgement and regular updates. We score the severity with CVSS v3.1 and develop the fix privately until it is ready to ship. Fixes go out in the next scheduled release, or in an out-of-schedule release if the issue is critical and affects all deployments. We note in the release notes when a release includes security fixes, and those notes go to the civiform-announce and civiform-technical lists. We decide case by case whether to publish a GitHub security advisory.

## Software Bill of Materials

The US Cybersecurity & Infrastructure Security Agency (CISA) recommends the inclusion of a Software Bill of Materials (SBOM). We create the SBOM file with each release. It can be found on the [releases page](https://github.com/civiform/civiform/releases).

For more information on the SBOM visit the [US National Telecommunications and Information Administration website](https://www.ntia.gov/page/software-bill-materials)
