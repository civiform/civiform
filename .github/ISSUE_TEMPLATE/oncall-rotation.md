---
name: Oncall rotation
about: Two week oncall rotation checklist and log
title: 'Oncall: 2026-XX-XX'
labels: 'oncall'
assignees: ''
---

The Oncall is responsible for drafting and releasing a new version of CiviForm during their two week rotation and general upkeep through their rotation. This is an aid for each rotation, but the docs are the source of truth.

Rotations begin every other Monday, with the draft release created on the first Tuesday and published the following Tuesday.

Please check off all items at their appropriate times during your rotation.

Resources:

- [Oncall Guide](https://github.com/civiform/civiform/wiki/On-Call-Guide#on-call-responsibilities)
- [Release Guide](https://github.com/civiform/civiform/wiki/Releasing)

# Task list:

## Release

- [ ] Choose a new version number in the format `vX.Y.Z`. If this is an unscheduled release in order to fix a bug, increment Z. Otherwise, increment Y and set Z to 0. Do not increment X without a discussion with the rest of the engineering team.
- [ ] Create a Draft Release by 12 PT on Tuesday during the first week of your shift
  - Ensure both the `civiform` and `cloud-deploy-infra` repos are tagged appropriately after running the `Create Release` action.
- [ ] Contact Matthew Sprenke for QA on Slack
- [ ] If Matthew is unavailable have the general team attempt a best-effort QA.
- [ ] Publish the release on Tuesday of the second week of your shift after it has gone through QA
- [ ] If the release is going to be skipped or published later than Wednesday EOD, email civiform-announce@googlegroups.com and civiform-technical@googlegroups.com to let admins know when to expect the next release.
- [ ] After publishing, email release notes
  - The email should include both a link to the release notes on GitHub, and a sentence or two summarizing what is in the release. It should be friendly to non-technical readers (think CiviForm and Program Admins). Feel free to use phrases like "small usability improvements" rather than listing each improvement. Edit the GitHub release notes and add this small summary to the top of it as well.
- [ ] Upgrade the demo site config files to the latest version by running [this action](https://github.com/civiform/civiform-staging-deploy/actions/workflows/update_demo_versions.yaml) then approving and merging the [generated PR](https://github.com/civiform/civiform-staging-deploy/pulls).

## General

- [ ] Create an Oncall issue for the next rotation, and assign to the next oncall
- [ ] Check Security lists daily
- [ ] Check #eng-ci Slack channel daily to monitor failed pushes and e2e test runs
- [ ] Once per shift, check Transifex to see if strings are ready to be sent for translation. See [Managing Translations](https://github.com/civiform/civiform/wiki/Exygy-Admin-Info#managing-translations) for detailed instructions.
- [ ] Make a weeks worth of progress (4 days) on an issue from the [On Call board](https://github.com/orgs/civiform/projects/1/views/95), a broken dependency update (see below), or a more urgent issue if one was assigned to you from the EM or PM.
- [ ] Check for dependency updates
  - Resolve mergeable dependency updates a few times per shift in batches of 3-5.
  - If there are broken dependency updates, pick one to look into during your shift. If it's a small fix that you can do in < 2 days, fix it and merge it.
  - Otherwise, create a new GitHub issue for the dependency update and document your findings. Ping Rocky so the issue gets added to our roadmap, mark the Renovate PR as "draft", and add a comment on the PR with the link to the new issue.
  - If you have time/interest, feel free to look into other broken dependencies and write comments on the PRs with your findings.
  - Review our [oncall docs](https://github.com/civiform/civiform/wiki/On-Call-Guide#dependency-updates) for more guidance and best practices for merging dependency updates.
  - [ ] Renovate PRs for [civiform/civiform](https://github.com/civiform/civiform/pulls/app%2Frenovate)
  - [ ] Dependabot PRs for [civiform/civiform](https://github.com/civiform/civiform/pulls/app%2Fdependabot)
  - [ ] Renovate PRs for [civiform/civiform-staging-deploy](https://github.com/civiform/civiform-staging-deploy/pulls/app%2Frenovate)
  - [ ] Renovate PRs for [civiform/cloud-deploy-infra](https://github.com/civiform/cloud-deploy-infra/pulls/app%2Frenovate)
