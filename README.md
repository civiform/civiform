[![ci](https://github.com/civiform/civiform/actions/workflows/push_tests.yaml/badge.svg)](https://github.com/civiform/civiform/actions/workflows/push_tests.yaml)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/6008/badge)](https://bestpractices.coreinfrastructure.org/projects/6008)
[![AWS Staging Deploy](https://github.com/civiform/civiform-staging-deploy/actions/workflows/aws_deploy.yaml/badge.svg?branch=main)](https://github.com/civiform/civiform-staging-deploy/actions/workflows/aws_deploy.yaml)
![codecov.io](https://codecov.io/github/civiform/civiform/coverage.svg?branch=main)
[![Code Style: Google](https://img.shields.io/badge/code%20style-google-blueviolet.svg)](https://google.github.io/styleguide/)

# CiviForm

This repository focuses on addressing **Improved Error Messaging (Feature #8031)** when adding a program admin. The current error message is confusing when a program admin has not yet logged in to CiviForm, especially regarding the differences between a CiviForm Admin and a Program Admin. This update aims to provide clearer instructions and prevent confusion.

## Problem
When attempting to add a program admin before they've logged in to CiviForm, the current error message does not clearly explain:
- Why the program admin must log in first.
- The difference between CiviForm Admin, Program Admin, and general "admin accounts."
- That the user will not have access to anything until they are granted access to a program.

This is important because CiviForm Admin feedback from Arkansas has highlighted this issue as frustrating, though they have a workaround.

## Solution
We aim to implement a clearer error message that explains:
- The reason the program admin must log in first.
- The distinction between different admin roles.
- A notice that users won’t have access to programs until granted access.

## Collaborators

- [Mathias Osiris (@LordSkyGod)](https://github.com/LordSkyGod)
- [Mateo Lauzardos (@MateoLauzardo)](https://github.com/MateoLauzardo)
- [Leticia Coto (@Lcoto05)](https://github.com/Lcoto05)

## Contributing

To get started, please first read our [Technical contribution guide](https://github.com/civiform/civiform/wiki/Technical-contribution-guide).

If you're interested in just digging around and interacting with the code, see
[Getting started](https://github.com/civiform/civiform/wiki/Getting-started) for guidance on
setting up your environment and running a local development server.
