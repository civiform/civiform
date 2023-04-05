[![ci](https://github.com/civiform/civiform/actions/workflows/push_tests.yaml/badge.svg)](https://github.com/civiform/civiform/actions/workflows/push_tests.yaml)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/6008/badge)](https://bestpractices.coreinfrastructure.org/projects/6008)
[![Seattle Staging Deploy](https://github.com/seattle-uat/civiform-deploy/actions/workflows/deploy-staging.yml/badge.svg?branch=main)](https://github.com/seattle-uat/civiform-deploy/actions/workflows/deploy-staging.yml)
[![AWS Staging Deploy](https://github.com/civiform/civiform-staging-deploy/actions/workflows/aws_deploy.yaml/badge.svg?branch=main)](https://github.com/civiform/civiform-staging-deploy/actions/workflows/aws_deploy.yaml)
![codecov.io](https://codecov.io/github/civiform/civiform/coverage.svg?branch=main)
[![Code Style: Google](https://img.shields.io/badge/code%20style-google-blueviolet.svg)](https://google.github.io/styleguide/)

# CiviForm

CiviForm aims to simplify the application process for benefits programs by re-using applicant data
for multiple benefits applications. It is being developed by Google.org in collaboration with the
City of Seattle.

Key features:

- No-code questionnaire definitions: admins can add new questions and programs using the UI without the need for custom code
- No-code predicate logic (for e.g. for conditionally requiring questions)
- No-code multi-language support through the admin UI
- Bulk data export with admin-defined privacy settings to preserve applicant privacy
- Trusted intermediary role to enable community based organizations to manage applications on behalf of clients
- Recursive data model: admins can define repeated and recursive questions for nested data such as asking for each address for each employer of each member of a household

See [an end-to-end demo](https://www.youtube.com/watch?v=AIYZEd5WAcU)

## Contributing

To get started please first read our
[Contributing](https://docs.civiform.us/contributor-guide/developer-guide/technical-contribution-guide#getting-up-to-speed) wiki page.

And more specifically:

- [Technical contribution guide](https://docs.civiform.us/contributor-guide/developer-guide)
- [UX contribution guide](https://docs.civiform.us/contributor-guide/ui-ux-guide)

If you're interested in just digging around and interacting with the code, see
[Getting started](https://docs.civiform.us/contributor-guide/developer-guide/getting-started) for guidance on
setting up your environment and running a local development server.

