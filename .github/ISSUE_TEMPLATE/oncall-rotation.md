---
name: Oncall rotation
about: Weekly oncall rotation checklist and log
title: 'Oncall: 2023-XX-XX'
labels: ''
assignees: ''
---

The Oncall is responsible for releasing a new version of CiviForm on Wednesday during their rotation and general upkeep through their rotation. This is an aid for each rotation, but the docs are the source of truth.

Rotations begin each Monday, with the release happening on Wednesday.

Please check off all items at their appropriate times during your rotation.

Resources:

- [Oncall Guide](https://docs.civiform.us/governance-and-management/project-management/on-call-guide#on-call-responsibilities)
- [Release Guide](https://docs.civiform.us/contributor-guide/developer-guide/releasing)

# Task list:

## Release

- [ ] Choose a new version number in the format `vX.Y.Z`. If any of the following apply to the release, bump the minor version (Y). Otherwise, bump the patch version (Z). Do not bump the major version (X) without discussing with #engineering, as this will need more extensive communication and planning.
  - A brand new feature (both when adding the feature behind a feature flag, and when removing the feature flag)
  - New APIs or API version (e.g. `/v2/` in the path instead of `/v1/`)
  - A database evolution
  - File key naming changes
  - Changes to other stateful parts of the application (i.e. changes to the format of things stored in the database not necessarily requiring an evolution)
  - Config setting addition or removal
- [ ] Create a Draft Release by 12 PT Wed
- [ ] Contact Matthew Sprenke for QA on Slack
- [ ] If Matthew is unavailable have the general team attempt a best-effort QA.
- [ ] After QA, publish the release
- [ ] Email release notes

## General

- [ ] Create an Oncall issue for the next rotation, and assign to the next oncall
- [ ] Check Security lists daily
- [ ] Check #ci Slack channel daily to monitor failed pushes
- [ ] Check [Dependency updates](https://github.com/civiform/civiform/pulls/app%2Frenovate) once
