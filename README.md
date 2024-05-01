[![ci](https://github.com/civiform/civiform/actions/workflows/push_tests.yaml/badge.svg)](https://github.com/civiform/civiform/actions/workflows/push_tests.yaml)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/6008/badge)](https://bestpractices.coreinfrastructure.org/projects/6008)
[![AWS Staging Deploy](https://github.com/civiform/civiform-staging-deploy/actions/workflows/aws_deploy.yaml/badge.svg?branch=main)](https://github.com/civiform/civiform-staging-deploy/actions/workflows/aws_deploy.yaml)
![codecov.io](https://codecov.io/github/civiform/civiform/coverage.svg?branch=main)
[![Code Style: Google](https://img.shields.io/badge/code%20style-google-blueviolet.svg)](https://google.github.io/styleguide/)

# CiviForm

CiviForm aims to simplify the application process for benefits programs by re-using applicant data
for multiple benefits applications. It is being developed by [Google.org](https://www.google.org/)
in collaboration with [Exygy](https://www.exygy.com/) and the [City of Seattle](https://www.seattle.gov/tech).

Key features:

- No-code questionnaire definitions: admins can add new questions and programs using the UI without the need for custom code
- No-code conditional logic for eligibility requirements
- No-code multi-language support through the admin UI
- Address correction and service area validation
- Bulk data export with admin-defined privacy settings to preserve applicant privacy
- Trusted intermediary role to enable community based organizations to manage applications on behalf of clients
- Recursive data model: admins can define repeated and recursive questions for nested data such as asking for each address for each employer of each member of a household

## Contributing

To get started please first read our [Technical contribution guide](https://github.com/civiform/civiform/wiki/Technical-contribution-guide).

If you're interested in just digging around and interacting with the code, see
[Getting started](https://github.com/civiform/civiform/wiki/Getting-started) for guidance on
setting up your environment and running a local development server.
