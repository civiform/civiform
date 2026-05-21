---
name: Feature Flag
about: Create a tracking issue for the lifecycle of a feature flag.
title: ''
labels: 'feature-flag'
assignees: ''
---

### Flag name

_Add the string used as the feature flag name here_

### Feature description

_What feature is this flag guarding?_

### Checklist

Follow detailed instructions in [Feature Flags wiki page](https://github.com/civiform/civiform/wiki/Feature-Flags), and keep the [feature flag tracking spreadsheet](https://docs.google.com/spreadsheets/d/1QYGrfpZvthu58HFutbE_-9kz-JVtp4AuCkCI3dsTbvs/edit?pli=1&gid=1720642859#gid=1720642859) updated as the flag lifecycle progresses.

- [ ] Create feature flag
- [ ] Implement code guarded with flag, including unit and browser tests that manipulate the state of the flag as needed
- [ ] Enable flag [in dev](https://github.com/civiform/civiform/blob/main/server/conf/application.dev.conf)
- [ ] Enable flag in [staging](https://github.com/civiform/civiform-staging-deploy/blob/main/aws_staging_civiform_config.sh), [QA](https://github.com/civiform/civiform-staging-deploy/blob/main/qa_civiform_config.sh), [eng](https://github.com/civiform/civiform-staging-deploy/blob/main/civiform_eng_civiform_config.sh), and [demo](https://github.com/civiform/civiform-staging-deploy/blob/main/civiform_demo_civiform_config.sh).
- [ ] Coordinate with @shreyachatterjee00 to announce the feature to governments
- [ ] Move the flag out of the "Experimental" section and into the "Feature Flags" section and remove the "(NOT FOR PRODUCTION USE)" warning from the description.
- [ ] Deprecate flag (default to true unless overridden in config)
- [ ] Remove flag completely
