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

## Developer setup

### TL;DR

1. Make code changes and add tests exercising the changes.
1. Run `bin/fmt`.
1. Run `bin/env-var-docs-create-venv`.
1. Run `bin/env-var-docs-run-tests`.

### Python dependencies

To contribute to any of the python files in this directory, ensure you have a
system installation of python version 3.10 or greater:

```sh
$ python3 --version
```

We use [virtual python
environments](https://docs.python.org/3/library/venv.html) (venv) to install
python dependencies rather than installing them in the system-wide packages.
Because a venv is just a directory, it is easier and less risky to manage its
lifecycle than the system-wide python installation. It also makes it less
likely that developers run into package version conflicts. For example, say you
had a project that requires PyGithub v1 and that version is installed in your
system-wide packages. Say generate_markdown.py requires PyGithub v2. To run
generate_markdown.py locally without a venv, you would need to upgrade the
system-wide version to v2. But then your other project would fail to run. Using
a venv allows us to install PyGithub v2 for generate_markdown.py while
retaining PyGithub v1 in your system-wide packages.

To set up a virtual environment with the required dependencies, run
`bin/env-var-docs-create-venv` from the repository root. This creates a venv in
`env-var-docs/venv`. To make the `python` and `pip` executables in your shell
point to the venv, run `source env-var-docs/venv/bin/activate`. To make them
point back to your system-wide installation, run `deactivate`. Re-running
`bin/env-var-docs-create-venv` will delete and re-create the virtual
environment. Doing so is a good way to get back to a known good state.

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
