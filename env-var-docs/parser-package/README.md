# Environment variable documentation file visitor

This directory contains a parser python module that supports parsing and
visiting nodes within an environment variable documentation file.

## Developer Setup

See [../README.md#developer-setup](../README.md#developer-setup).

### Python dependencies

The parser.py module itself does not depend on any third-party packages and
should be kept that way if possible. If a non-stdlib dependency is deemed
critical, add it to [./pyproject.toml](./pyproject.toml) in
`[project].dependencies`. Pinning an exact dependency version is encouraged to
minimize reproducability issues. For example:

```
[project]
...
dependencies = [
    "PyYAML==6.0"
]
```

We declare our developer dependencies in `[project.optional-dependencies]`. For
example, dependencies needed to run tests are in
`[project.optional-dependencies].test`.

### Environment variable documentation file format

An environment variable documentation file contains key-value pairs. Each key
is either a group name or the name of an environment variables referenced in
application.conf.

Groups have the following fields:

- `group_description`: A human-readable description of what the group
  configures.
- `members`: An object containing groups or variables.

The `group_description` and `members` fields must be set for each group.

Variables have the following fields:

- `description`: A human-readable description of what the environment variable
  configures.
- `type`: The value type expected. Can be 'string', 'int', 'bool', or
  '[index-list(#index-lists)'.
- `mode`: Determines where the variable is set and displayed. `HIDDEN` vars are
  set in the environment and not visible to admins, `ADMIN_READABLE` vars are set
  in the environment and displayed to the admin in the Settings UI, `ADMIN_WRITEABLE`
  vars are set by CiviForm admins in the CiviForm UI, `SECRET` vars are set in the
  environment by the cloud provider's secrets manager and not displayed to the admin.
- `required`: If the environment variable is required to be set in the
  [config.sh file](https://github.com/civiform/civiform-deploy/blob/957a90fc27b5816de464231d92dc5886dac26b93/civiform_config.example.sh). Attempting to deploy while missing variables marked "required" will throw an error.
  If the `required` field is not set, the environment variable is not required.
- `values`: If `type` is string, a list of valid strings can be provided. If
  `values` is defined, `regex` and `regex_tests` can not be defined.
- `regex`: If `type` is string, a regular expression using [python re
  syntax](https://docs.python.org/3/library/re.html#regular-expression-syntax)
  can be provided. This regular expression defines the set of valid values.
  Because the regex is in a JSON string, any " or \ characters in it need to be
  escaped like \\" or \\\. If `regex` is defined, `values` can not be defined.
  If `regex` is defined, `regex_tests` must also be defined.
- `regex_tests`: A list of objects. Each object contains a `val` field with a
  string value and a `should_match` field with a boolean value. This allows for
  presubmit checks to test each `val` against `regex` and ensure `should_match`
  is equal to the actual outcome. Tests reduce the likelihood of typos or
  errors in `regex`. At a minimum, at least two tests should be specified: one
  that matches the regular expression and one that does not. Depending on the
  complexity of `regex`, many more tests should be specified that test the
  corner-cases.

The `description`, `type`, and `mode` fields must be defined for each variable.

### Index lists

Index lists are an [implementation
detail](https://github.com/lightbend/config/blob/main/HOCON.md#conversion-of-numerically-indexed-objects-to-arrays)
of the HOCON format the application.conf file uses. They allow lists to be set
via environment variables. Each element in the list must be provided like:

```
MY_LIST.0 = "first item"
MY_LIST.1 = "second item"
```

### Example

Here is an example environment variable documentation file (validated in
[./tests/readme_test.py](./tests/readme_test.py)):

```env-var-docs-file-example
{
    "Branding": {
        "group_description": "Configures CiviForm branding.",
        "members": {
            "TITLE": {
                "description": "Sets the CiviForm title.",
                "type": "string",
                "mode": "ADMIN_READABLE"
            },
            "LOGO_URL": {
                "description": "URL of the logo.",
                "type": "string",
                "regex": "^https?://",
                "regex_tests": [
                    { "val": "http://mylogo.png", "should_match": true },
                    { "val": "https://my-secure-logo.png", "should_match": true },
                    { "val": "props-not-a-valid-URL", "should_match": false }
                ],
                "mode": "ADMIN_READABLE"
            }
        }
    },
    "SOME_NUMBER": {
        "description": "Sets a very important number.",
        "type": "int",
        "required": true,
        "mode": "HIDDEN"
    },
    "CLOUD_PROVIDER": {
        "description": "What cloud services to connect to.",
        "type": "string",
        "values": ["aws", "azure", "gcp"],
        "mode": "HIDDEN"
    },
    "LANGUAGES": {
        "description": "Supported languages.",
        "type": "index-list",
        "mode": "ADMIN_READABLE"
    }
}
```

Here is an example .env file that meets the above documentation validation rules:

```
TITLE="The title"
SOME_NUMBER=42
LOGO_URL="https://my-cool-logo.png"
CLOUD_PROVIDER="aws"
LANGUAGES.0="en-US"
LANGUAGES.1="en-PI"
```
