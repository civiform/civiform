---
name: Oncall rotation
about: Weekly oncall rotation checklist and log
title: 'Oncall: 2024-XX-XX'
labels: 'oncall'
assignees: ''
---

The Oncall is responsible for releasing a new version of CiviForm on Tuesday during their rotation and general upkeep through their rotation. This is an aid for each rotation, but the docs are the source of truth.

Rotations begin each Monday, with the release happening on Tuesday.

Please check off all items at their appropriate times during your rotation.

Resources:

- [Oncall Guide](https://docs.civiform.us/governance-and-management/project-management/on-call-guide#on-call-responsibilities)
- [Release Guide](https://github.com/civiform/civiform/wiki/Releasing)

# Task list:

## Release

- [ ] Choose a new version number in the format `vX.Y.Z`. If any of the following apply to the release, bump the minor version (Y). Otherwise, bump the patch version (Z). Do not bump the major version (X) without discussing with #engineering, as this will need more extensive communication and planning.
  - A brand new feature (both when adding the feature behind a feature flag, and when removing the feature flag)
  - New APIs or API version (e.g. `/v2/` in the path instead of `/v1/`)
  - A database evolution
  - File key naming changes
  - Changes to other stateful parts of the application (i.e. changes to the format of things stored in the database not necessarily requiring an evolution)
  - Config setting addition or removal
- [ ] Create a Draft Release by 12 PT on Tuesday
  - Ensure both the `civiform` and `cloud-deploy-infra` repos are tagged appropriately after running the `Create Release` action.
- [ ] Contact Matthew Sprenke for QA on Slack
- [ ] If Matthew is unavailable have the general team attempt a best-effort QA.
- [ ] After QA, publish the release
- [ ] Email release notes
  - The email should include both a link to the release notes on GitHub, and a sentence or two summarizing what is in the release. It should be friendly to non-technical readers (think CiviForm and Program Admins). Feel free to use phrases like "small usability improvements" rather than listing each improvement. Edit the GitHub release notes and add this small summary to the top of it as well.

## General

- [ ] Create an Oncall issue for the next rotation, and assign to the next oncall
- [ ] Check Security lists daily
- [ ] Check #ci Slack channel daily to monitor failed pushes
- [ ] Check [needs triage bugs](https://github.com/civiform/civiform/issues?q=is%3Aopen+is%3Aissue+label%3Aneeds-triage) daily to ensure there aren't any P0s
- [ ] Check [Dependency Dashboard](https://github.com/civiform/civiform/issues/2124) once
  - This issue is one that Renovate creates and updates with what it is currently tracking. Check this for any rate-limited dependencies and check the box to create them.
  - For any problematic dependency updates that break tests, add the "needs-triage" label so Exygy can prioritize fixing these issues.
