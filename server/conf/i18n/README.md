## Localization

The `server/conf/i18n` directory contains localization files for the CiviForm server.

`messages` represents Play's source strings. Each foreign language's strings are localized in a `messages.xx` file. If a string is missing in a foreign language's file, it will default to the source string.

Developers are free to add to and update `messages` with English strings as needed. When they do, the [Transifex CiviForm project](https://app.transifex.com/civiform/civiform/dashboard) will update with their changes upon merging to `main`. However, they should **not** modify the foreign language files. These files are managed by Transifex when translations are added.

Please see the [internationalization docs](<https://github.com/civiform/civiform/wiki/Internationalization-(i18n)>) for more information on how translations are retrieved and updated in CiviForm.


