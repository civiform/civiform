# Environment Variable Documentation

The main configuration file for the CiviForm server is in
[application.conf](../server/conf/application.conf). The file is in [HOCON
format](https://github.com/lightbend/config/blob/main/HOCON.md) which supports
reading values at server start time from environment variables. This is done
using the `${?SOME_ENV_VAR}` substitution syntax.

To aid in deploying the CiviForm server, we ensure every environment variable
referenced in the configuration file has appropriate documentation. This
documentation lives in [env-var-docs.json](../server/conf/env-var-docs.json).
See [./parser-package/README.md](./parser-package/README.md) for the expected
structure of env-var-docs.json. As part of a CiviForm release, the
documentation is rendered as markdown and added to our [documentation
website](https://docs.civiform.us/it-manual/sre-playbook/server-environment-variables).

The documentation includes support for adding value validation rules. Tools
that deploy the CiviForm server
([civiform/cloud-deploy-infra](https://github.com/civiform/cloud-deploy-infra),
for example) should use these rules to validate user-provided values before the
server is deployed.

## GitHub actions automations

- [check_vars_documented.py](./check_vars_documented.py): Ensures all
  environment variables referenced in application.conf are documented in
  env-var-docs.json and that env-var-docs.json does not document any variables
  not referenced in application.conf (ensures a 1:1 mapping). Runs on any PR
  changing application.conf or env-var-docs.json.

- [run_regex_tests.py](./run_regex_tests.py): Ensures that all regular
  expression validation rules in env-var-docs.json compile and that all
  provided regular expressions tests pass. Runs on any PR changing
  application.conf or env-var-docs.json.

- [generate_markdown.py](./generate_markdown.py): Generates markdown from
  env-var-docs.json and submits it to our [documentation
  repository](https://github.com/civiform/docs/tree/main/docs/it-manual/sre-playbook/server-environment-variables).
  Runs as a part of our release automation.

### Running locally

```sh
# Run check_vars_documented.py and run_regex_tests.py:
bin/env-var-docs-check-vars

# Run generate_markdown.py:
bin/env-var-docs-generate-markdown
```

## Developer setup

### TL;DR

1. Make code changes and add tests exercising the changes.
1. Run `bin/fmt`.
1. Run `bin/env-var-docs-run-tests`.

### Python dependencies

We use a Docker container to install dependencies and run actions, rather than
running python locally, to ensure the actions are always able to run, regardless
of the system version of python you have installed. Ensure that you have docker
installed and the `docker run` command is available.

### Tests

We run [mypy](https://mypy-lang.org/) to check any [type
hints](https://docs.python.org/3/library/typing.html) and
[pytest-cov](https://pypi.org/project/pytest-cov/) to ensure tests pass and to
generate code coverage.

Run `bin/env-var-docs-run-tests` to run these tests. You do not need to run
`source env-var-docs/venv/bin/activate` for this script to run successfully.

### Formatting

Running `bin/fmt` from the repository root will format the files in-place.

To integrate with your code editor, install
[yapf](https://github.com/google/yapf) and configure it with the following
options:

`--verbose --style='{based_on_style: google, SPLIT_BEFORE_FIRST_ARGUMENT:true}'`
