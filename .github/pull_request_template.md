### Description

Please explain the changes you made here.

## Release notes

Remove this section if the title of the pull request can be used as the default release notes description. If more detail is needed to communicate to partners the scope of the PR's changes, use this release notes section (and remove this placeholder text).

### Checklist

#### General

Read the full guidelines for PRs [here](https://github.com/civiform/civiform/wiki/Technical-contribution-guide#creating-a-pull-request)

- [ ] Added the correct label: < feature | enhancement | bug | under-development | dependencies | infrastructure | ignore-for-release | database >
- [ ] Assigned to a specific person, `civiform/developers`, or a [more specific round-robin list](https://github.com/civiform/civiform/wiki/Technical-contribution-guide#adding-reviewers)
- [ ] Added an additional reviewer from outside your organization as FYI (if the primary reviewer is in the same organization as you)
- [ ] Removed the release notes section if the title is sufficient for the release notes description, or put more details in that section.
- [ ] Created unit and/or browser tests which fail without the change (if possible)
- [ ] Performed manual testing (Chrome and Firefox if it includes front-end changes)
- [ ] Extended the README / documentation, if necessary. For user-facing features, consider updating [the user docs](https://github.com/civiform/docs). For "under-the-hood" changes or things more relevant to developers, consider updating [the dev wiki](https://github.com/civiform/civiform/wiki).

#### Database evolutions

Read the guidelines [here](https://github.com/civiform/civiform/wiki/Database#writing-database-evolutions)

- [ ] Assigned two reviewers
- [ ] Guarded against already existing resources using `IF NOT EXISTS` and `IF EXISTS`
- [ ] Downs created to undo changes in Ups
- [ ] Every comment in script should begin with -- and not # --- unless it denotes Ups or Downs. See [here](https://www.playframework.com/documentation/2.9.x/Evolutions) for details.
- [ ] Tested both the Downs and the Ups scripts manually (When testing, include all comments from the evolution in your test script to ensure any syntax errors in the comments are caught.)
- [ ] Data migrations aren't being done (please use a [Durable Job](https://github.com/civiform/civiform/wiki/Database#durable-jobs-for-data-updates) if doing a data migration)
- [ ] Update the model documentation in our [wiki](https://github.com/civiform/civiform/wiki/Database) if necessary

#### Durable jobs

Read the docs [here](https://github.com/civiform/civiform/wiki/Database#durable-jobs-for-data-updates)

- [ ] Assigned two reviewers
- [ ] Included a rollback plan and a job to undo the data changes if appropriate

#### User visible changes

- [ ] Followed steps to [internationalize](https://github.com/civiform/civiform/wiki/Internationalization-%28i18n%29#application-strings) any new strings
  - [ ] Added context strings to new [messages](https://github.com/civiform/civiform/blob/main/server/conf/i18n/messages)
  - [ ] Didn't use a message in applicant facing code that isn't translated yet (unless behind a flag)
- [ ] Wrote browser tests using the [validateAccessibility](https://sourcegraph.com/github.com/civiform/civiform/-/blob/browser-test/src/support/index.ts?L437:14&subtree=true) method
- [ ] Made equivalent changes in Thymeleaf for applicant-facing features and changes (or created an issue in the [Northstar epic](https://github.com/orgs/civiform/projects/1/views/94) to track that it's missing from Thymeleaf)
- [ ] Tested on mobile view. See [mobile device mode](https://developer.chrome.com/docs/devtools/device-mode/)
- [ ] Manually tested at 200% size
- [ ] Manually evaluated tab order
- [ ] Manually tested with a screen reader if the feature is applicant-facing. See [screen reader testing](https://github.com/civiform/civiform/wiki/Testing#screen-reader-testing)

#### New Features

- [ ] Add new FeatureFlag env vars to `server/conf/helper/feature-flags.conf`
- [ ] Conditioned new functionality on a [FeatureFlag](https://github.com/civiform/civiform/wiki/Feature-Flags)
- [ ] Wrote browser tests with the feature flag off and on, etc.

### Instructions for manual testing

If instructions are needed for manual testing by reviewers, include them here.

### Issue(s) this completes

Fixes #<issue_number>; Fixes #<issue_number>...
