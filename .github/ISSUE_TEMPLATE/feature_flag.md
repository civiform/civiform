---
name: Feature Flag
about: Create a tracking issue for the creation, deployment, and deletion of a feature flag.
title: ''
labels: 'feature-flag'
assignees: ''
---

See [Releasing a Feature Behind a Feature Flag](https://github.com/civiform/civiform/wiki/Feature-Flags#releasing-a-feature-behind-a-feature-flag) for more details.

### Flag name

Add the string used as the feature flag name here

### Feature description

What feature is this flag guarding?

### Checklist

- [ ] Feature flag created as `ADMIN_WRITEABLE`, feature guarded with flag in code
- [ ] Flag [enabled in dev](https://github.com/civiform/civiform/blob/main/server/conf/application.dev.conf)
- [ ] Flag [disabled for browser tests](https://github.com/civiform/civiform/blob/main/server/conf/application.dev-browser-tests.conf)
- [ ] Feature fully written and guarded with flag, including unit and browser tests that manipulate the state of the flag as needed. Enable the flag for browser tests when it's ready to be tested.
- [ ] Flag enabled in staging
- [ ] Coordinated with @shreyachatterjee00 to notify CEs of new feature and ask them to turn it on and try it out in their staging
- [ ] Coordinated with @shreyachatterjee00 to communicate to CEs that we are going to turn the flag on in production by default, ensuring CEs have had a chance to test out the feature and provide feedback.
- [ ] Flag type changed to `ADMIN_READABLE` and default changed to true, with PR release notes mentioning the change and what feature it enables. <Edit this issue and note the date it was enabled here>
- [ ] Verified feature is enabled in one or more production builds. <Edit this issue and note the date it was enabled here>
- [ ] Flag enabled in production for at least a month
- [ ] Flag removed and feature code unguarded
