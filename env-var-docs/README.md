# Environment Variable Documentation

The main configuration file for the CiviForm server is in
[application.conf](../server/conf/application.conf). The file is in [HOCON
format](https://github.com/lightbend/config/blob/main/HOCON.md) which supports
reading values at server start time from environment variables. This is done
using the `${?SOME_ENV_VAR}` substitution syntax.

To aid in deploying the CiviForm server, we ensure every environment variable
referenced in the configuration file has appropriate documentation. This
documentation lives in [env-var-docs.json](../server/conf/env-var-docs.json).
See [./env_var_docs/schema/README.md](./env_var_docs/schema/README.md) for
documentation on the format. As part of a CiviForm release, the documentation
is rendered as markdown and added to our [documentation
website](https://docs.civiform.us/it-manual/sre-playbook/server-environment-variables).

## Scripts

- [check_vars_documentation.py](./check_vars_documented.py): Ensures all
  environment variables referenced in application.conf are defined in
  env-var-docs.json and that env-var-docs.json does not document any variables
  referenced in application.conf. Runs on any PR changing application.conf or
  env-var-docs.json.

- [run_regex_tests.py](./run_regex_tests.py): Ensures that all regular
  expression validation rules in env-var-docs.json compile and that all
  provided regualar expressions tests pass. Runs on any PR changing
  application.conf or env-var-docs.json.

- [generate_markdown.py](./generate_markdown.py): Generates markdown from
  env-var-docs.json and submits it to our [documentation
  repository](https://github.com/civiform/docs/tree/main/docs/it-manual/sre-playbook/server-environment-variables).
  Runs as a part of our release automation.

## Developer setup

To contribute to any of the python files in this directory, ensure you have a
system installation of python version 3.10 or greater:

```sh
$ python3 --version
```

### Installing code dependencies

Create a [virtual python
environment](https://docs.python.org/3/library/venv.html). From the repository
root, change your shell's working directory:

```sh
$ cd env-var-docs
$ python3 -m venv venv
```

Activate the new virtual environment:

```sh
$ source venv/bin/activate
```

If you are ever unsure if you are using the system python installation or a
virtual environment, run:

```sh
$ which python
```

The path should point to the created virtual environment in this directory.

Install the dependencies:

```sh
$ pip install ./env_var_docs
$ pip install -r requirements.txt
```

To exit the virtual environment, run:

```sh
$ deactivate
```

### Tooling dependencies

We run a formatter and type checker on all PRs before they are allowed to be merged.

#### Formatting

We use [yapf](https://github.com/google/yapf) with the following options:

`--verbose --style='{based_on_style: google, SPLIT_BEFORE_FIRST_ARGUMENT:true}'`

See in the `python-formatting` job in the [format action](.github/workflows/format.yaml).

#### Type checking

We use [mypy](https://github.com/python/mypy) to check python [type
hints](https://docs.python.org/3/library/typing.html). We use the default
options.
