# Environment variable documentation file format

We maintain a [json-schema](https://json-schema.org/) that defines the expected
structure of env-var-docs.json in [schema.json](./schema.json).

The env-var-docs.json file contains key-value pairs. Each key is either a group
of environment variables or an environment variable referenced in
application.conf.

Groups have the following fields:

- `group-description`: A human-readable description of what the group
  configures.
- `members`: An object containing groups or variables.

The `group-description` and `members` fields must be set for each group.

Variables have the following fields:

- `description`: A human-readable description of what the environment variable
  configures.
- `type`: The value type expected. Can be 'string', 'int', 'bool', or
  'index-list'.
- `required`: If the environment variable is required to be set. If the
  `required` field is not set, the environment variable is not required.
- `values`: If `type` is string, a list of valid strings can be provided. If
  `values` is defined, `regex` and `regex_tests` can not be defined.
- `regex`: If `type` is string, a regular expression using [python re
  syntax](https://docs.python.org/3/library/re.html#regular-expression-syntax)
  can be provided. This regular expression defines the set of valid values.
  Because the regex is in a JSON string, any " or \ characters in it need to be
  escaped like \" or \\. If `regex` is defined, `values` can not be defined.
- `regex_tests`: A list of objects. Each object contains a `val` field with a
  string value and a `shouldMatch` field with a boolean value. If `regex` is
  defined, `regex_tests` must also be defined.

The `description` and `type` fields must be set for each variable.

## Index lists

Index lists are an [implementation
detail](https://github.com/lightbend/config/blob/main/HOCON.md#conversion-of-numerically-indexed-objects-to-arrays)
of the HOCON format the application.conf file uses. They allow lists to be set
via environment variables. Each element in the list must be provided like:

```
MY_LIST.0 = "first item"
MY_LIST.1 = "second item"
```

## Example

Here is an example env-var-docs.json file:

```
{
    "TITLE": {
        "description": "Sets the CiviForm title.",
        "type": "string"
    },
    "SOME_NUMBER": {
        "description": "Sets a very important number.",
        "type": "int",
        "required": true
    },
    "LOGO_URL": {
        "description": "URL of the logo.",
        "type": "string",
        "regex": "^https?://",
        "regex_tests": [
            { "val": "http://mylogo.png", "shouldMatch": true },
            { "val": "https://my-secure-logo.png", "shouldMatch": true },
            { "val": "props-not-a-valid-URL", "shouldMatch": false }
        ]
    },
    "CLOUD_PROVIDER": {
        "description": "What cloud services to connect to.",
        "type": "string",
        "values": ["aws", "azure", "gcp"]
    }
    "LANGUAGES": {
        "description": "Supported languages.",
        "type": "index-list"
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
