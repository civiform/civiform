---
name: Feature Flag
about: Create a tracking issue for the creation, deployment, and deletion of a feature flag.
title: ''
labels: 'feature-flag'
assignees: ''
---

See https://docs.civiform.us/contributor-guide/developer-guide/feature-flags#releasing-a-feature-behind-a-feature-flag for more details.

### Flag name

Add the string used as the feature flag name here

### Feature description

What feature is this flag guarding?

### Checklist

- [ ] Feature flag created, feature guarded with flag in code
- [ ] Flag [enabled in dev](https://github.com/civiform/civiform/blob/main/server/conf/application.dev.conf)
- [ ] Flag [disabled for browser tests](https://github.com/civiform/civiform/blob/main/server/conf/application.dev-browser-tests.conf)
- [ ] Feature written and guarded with flag, including unit and browser tests that manipulate the state of the flag as needed
- [ ] Flag passed through to deployment system ([example](https://github.com/civiform/cloud-deploy-infra/commit/9d17356ff1fa1f3a16c97608cc00cbd4c7c11ffe))
- [ ] Flag enabled in staging and Seattle staging
- [ ] Seattle given option to test manually
- [ ] Flag default changed to true, with PR release notes mentioning the change and what feature it enables. <Edit this issue and note the date it was enabled here>
- [ ] Verified feature is enabled in production build. <Edit this issue and note the date it was enabled here>
- [ ] Flag enabled in production for at least a month
- [ ] Flag removed and feature code unguarded
