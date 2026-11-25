# Security Policy

## Supported Versions

See our [documentation on release versioning](https://docs.civiform.us/it-manual/sre-playbook/upgrading-to-a-new-release).

We cut a release about every two weeks. All production deployments are encouraged to upgrade with each release and keep in regular communication with the development team.

## Reporting a Vulnerability

Please report vulnerabilities privately through GitHub's ["Report a vulnerability" form](https://github.com/civiform/civiform/security/advisories/new). This goes directly to the maintainers and lets us coordinate a fix with you before disclosure.

Please include the docker image tag for the version in which you have found the vulnerability, or a link to code on GitHub if that is more appropriate.

## Software Bill of Materials

The US Cybersecurity & Infrastructure Security Agency (CISA) recommends the inclusion of a Software Bill of Materials (SBOM). We create the SBOM file with each release. It can be found on the [releases page](https://github.com/civiform/civiform/releases).

For more information on the SBOM visit the [US National Telecommunications and Information Administration website](https://www.ntia.gov/page/software-bill-materials)

## Verifying a release

Each release also has a signature, so you can check that the image you run is the one we published. The [releases page](https://github.com/civiform/civiform/releases) lists two files for each release, for example `civiform-civiform_v3_38_0.digest.txt` and `civiform-civiform_v3_38_0.digest.txt.sigstore.json`. The first names the release image by its tag and digest, and the second is its [Sigstore](https://www.sigstore.dev/) signature. We sign with the identity of our `sign_release.yaml` GitHub Actions workflow, so there are no keys to manage.

To verify a release, download both files and run [cosign](https://docs.sigstore.dev/cosign/system_config/installation/) (v3 or later):

```bash
cosign verify-blob \
  --bundle civiform-civiform_v3_38_0.digest.txt.sigstore.json \
  --certificate-identity-regexp '^https://github\.com/civiform/civiform/\.github/workflows/sign_release\.yaml@refs/(heads/main|heads/release/.+|tags/v[0-9]+\.[0-9]+\.[0-9]+)$' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  civiform-civiform_v3_38_0.digest.txt
```

Then check that the image you run has the digest in that file, for example with `docker buildx imagetools inspect docker.io/civiform/civiform:v3.38.0`.
