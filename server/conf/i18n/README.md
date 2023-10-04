## Localization

The `server/conf/i18n` directory contains localization files for the CiviForm server.

`messages` represents Play's source strings. Each foreign language's strings are localized in a `messages.xx` file. If a string is missing in a foreign language's file, it will default to the source string.

Developers are free to add to and update `messages` with English strings as needed. When they do, the [Transifex CiviForm project](https://app.transifex.com/civiform/civiform/dashboard) will update with their changes upon merging to `main`. However, they should **not** modify the foreign language files. These files are managed by Transifex when translations are added.

Please see the [internationalization docs](<https://github.com/civiform/civiform/wiki/Internationalization-(i18n)>) for more information on how translations are retrieved and updated in CiviForm.

## Updating the `messages` file

When updating this file:

- Keep in mind that translators do not have all of CiviForm's context in mind. To help them, please include a description **to the [Transifex dashboard](https://app.transifex.com/civiform/civiform/dashboard/)** that allows someone without knowledge about where this string is being used to understand it fully.
  - For example, explain where the string is shown and if strings are deployment-specific, specify what each passed through value represents, such as: "Shown to applicants to encourage login; {0} represents the civic entity's name, in reference to their login portal."
  - See https://user-images.githubusercontent.com/30369272/239957567-5ddd5c42-4194-488a-9b1e-61cb4ec33c8b.png
- Apostrophes in translations must be escaped by another apostrophe. See https://www.playframework.com/documentation/2.8.x/JavaI18N#Notes-on-apostrophes
- Sections correspond to a single view, such as the applicant's home page or a view of an address question.
- We use the following key format: `${component_type}.${descriptive_name}`
  - For example, `button.nextPage` would contain a translation for a button used to go to the next page. `button.ariaLabel.delete` would contain a translation a screen reader could read for the delete button.
- Keys in this file should be alphabetized within their own section or sub-section.
