---
name: Feature Flag
about: Create a tracking issue for the creation, deployment, and deletion of a feature flag.
title: ''
labels: 'feature-flag'
assignees: ''
---

### Flag name

*Add the string used as the feature flag name here*

### Feature description

*What feature is this flag guarding?*

### Checklist

Follow detailed instructions in [Feature Flags wiki page](https://github.com/civiform/civiform/wiki/Feature-Flags), and keep the [feature flag tracking spreadsheet](https://docs.google.com/spreadsheets/d/149xPiY0WMLAE5be51pvznyytfGVRTzu4QqrBs427DaA/edit#gid=0) updated as the flag lifecycle progresses.

- [ ] Create feature flag
- [ ] Implement code guarded with flag, including unit and browser tests that manipulate the state of the flag as needed
- [ ] Enable flag [in dev](https://github.com/civiform/civiform/blob/main/server/conf/application.dev.conf)
- [ ] Enable flag [in staging](https://github.com/civiform/civiform-staging-deploy/blob/main/aws_staging_civiform_config.sh)
- [ ] Coordinate with @shreyachatterjee00 to notify CEs to enable and test the feature in staging and then production
- [ ] Deprecate flag (default to true unless overridden in config)
- [ ] Remove flag completely
