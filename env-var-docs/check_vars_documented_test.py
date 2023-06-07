from check_vars_documented import make_config, vars_from_application_conf, vars_from_docs, main
import contextlib
import io
import os
import tempfile
import unittest


class TestValidateEnvVariables(unittest.TestCase):

    def setUp(self):
        os.environ = {}

    def test_no_app_conf(self):
        stderr = io.StringIO()
        with tempfile.NamedTemporaryFile() as tf, contextlib.redirect_stderr(
                stderr):
            os.environ["ENV_VAR_DOCS_PATH"] = tf.name
            with self.assertRaises(SystemExit):
                make_config()
            self.assertTrue(
                "APPLICATION_CONF_PATH must be present in the environment variables"
                in stderr.getvalue())

    def test_app_conf_is_dir(self):
        stderr = io.StringIO()
        with tempfile.TemporaryDirectory(
        ) as dir_name, contextlib.redirect_stderr(stderr):
            os.environ["APPLICATION_CONF_PATH"] = dir_name
            with self.assertRaises(SystemExit):
                make_config()
            self.assertTrue("does not point to a file" in stderr.getvalue())

    def test_no_env_var(self):
        stderr = io.StringIO()
        with tempfile.NamedTemporaryFile() as tf, contextlib.redirect_stderr(
                stderr):
            os.environ["APPLICATION_CONF_PATH"] = tf.name
            with self.assertRaises(SystemExit):
                make_config()
            self.assertTrue(
                "ENV_VAR_DOCS_PATH must be present in the environment variables"
                in stderr.getvalue())

    def test_config_returned(self):
        with tempfile.NamedTemporaryFile(
        ) as appconf, tempfile.NamedTemporaryFile() as envvar:
            os.environ["APPLICATION_CONF_PATH"] = appconf.name
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            got = make_config()
            self.assertEqual(got.app_conf_path, appconf.name)
            self.assertEqual(got.docs_path, envvar.name)


class TestVarsFromAppConf(unittest.TestCase):

    def setUp(self):
        self.no_vars = """
#mykey = ${some.value}
# mykey = ${?JAVA_HOME}
// mykey = ${?JAVA_HOME}
akka {
  #log-config-on-start = ${?LOG_CONFIG_ON_START}
  #log-config-on-start = true
  logger-startup-timeout = 30s
}"""

        self.vars_1 = """
my_var_1 = ${?MY_VAR_1} # Some var
my_var_2 = ${? MY_VAR_2   }"""

        self.vars_2 = """
my_var_3 = ${?MY_VAR_3}
my_var_4 = ${?MY_VAR_4}"""

        self.blank_imports_no_vars = 'include "no_vars"'
        self.blank_imports_vars_1 = 'include "vars_1"'

        self.vars_1_imports_no_vars = 'include "no_vars"' + self.vars_1
        self.vars_1_imports_vars_2 = 'include "vars_2"' + self.vars_1
        self.vars_2_imports_vars_1 = 'include "vars_1"' + self.vars_2

        self.blank_imports_vars_1_and_vars_2 = 'include "vars_1"\ninclude "vars_2"'

        self.vars_3_imports_vars_1_and_vars_2 = """
include "vars_1"
include "vars_2"

my_var_5 = ${?MY_VAR_5}
my_var_6 = ${?MY_VAR_6}"""

        self.vars_1_set = set(["MY_VAR_1", "MY_VAR_2"])
        self.vars_2_set = set(["MY_VAR_3", "MY_VAR_4"])
        self.vars_3_set = set(["MY_VAR_5", "MY_VAR_6"])

        self.conf_dir = tempfile.TemporaryDirectory()
        for n in ["no_vars", "vars_1", "vars_2", "blank_imports_no_vars",
                  "blank_imports_vars_1", "vars_1_imports_no_vars",
                  "vars_1_imports_vars_2", "vars_2_imports_vars_1",
                  "blank_imports_vars_1_and_vars_2",
                  "vars_3_imports_vars_1_and_vars_2"]:
            with open(self.temp_path(n), mode="w") as f:
                f.write(getattr(self, n))

    def temp_path(self, name):
        """Returns a path in self.conf_dir. Should only be called after
        self.conf_dir has been initialized."""

        return f"{self.conf_dir.name}/{name}"

    def test_no_vars(self):
        got = vars_from_application_conf(self.temp_path("no_vars"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, set())

    def test_vars_1(self):
        got = vars_from_application_conf(self.temp_path("vars_1"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, self.vars_1_set)

    def test_vars_2(self):
        got = vars_from_application_conf(self.temp_path("vars_2"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, self.vars_2_set)

    def test_blank_imports_no_vars(self):
        got = vars_from_application_conf(
            self.temp_path("blank_imports_no_vars"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, set())

    def test_blank_imports_vars_1(self):
        got = vars_from_application_conf(self.temp_path("blank_imports_vars_1"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, self.vars_1_set)

    def test_vars_1_imports_no_vars(self):
        got = vars_from_application_conf(
            self.temp_path("vars_1_imports_no_vars"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, self.vars_1_set)

    def test_vars_1_imports_vars_2(self):
        got = vars_from_application_conf(
            self.temp_path("vars_1_imports_vars_2"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, self.vars_1_set | self.vars_2_set)

    def test_vars_2_imports_vars_1(self):
        got = vars_from_application_conf(
            self.temp_path("vars_2_imports_vars_1"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, self.vars_1_set | self.vars_2_set)

    def test_blank_imports_vars_1_and_vars_2(self):
        got = vars_from_application_conf(
            self.temp_path("blank_imports_vars_1_and_vars_2"))
        result_set = set(got.keys())
        self.assertSetEqual(result_set, self.vars_1_set | self.vars_2_set)

    def test_vars_3_imports_vars_1_and_vars_2(self):
        got = vars_from_application_conf(
            self.temp_path("vars_3_imports_vars_1_and_vars_2"))
        result_set = set(got.keys())
        self.assertSetEqual(
            result_set, self.vars_1_set | self.vars_2_set | self.vars_3_set)


class TestVarsFromDocs(unittest.TestCase):
    env_var_docs = """
    {
        "MY_VAR": {
            "description": "A var",
            "type": "string",
            "mode": "HIDDEN"
        },
        "A group": {
            "group_description": "Some group",
            "members": {
                "MY_OTHER_VAR": {
                    "description": "Another var",
                    "type": "string",
                    "mode": "HIDDEN"
                }
            }
        }
    }
    """

    def test_no_vars(self):
        with io.StringIO("{}") as f:
            got, gotErrors = vars_from_docs(f)
            self.assertEqual(gotErrors, [])
            self.assertSetEqual(set(got.values()), set())

    def test_some_vars(self):
        with io.StringIO(self.env_var_docs) as f:
            got, gotErrors = vars_from_docs(f)
            self.assertEqual(gotErrors, [])
            self.assertSetEqual(
                set(got.keys()), set(["MY_VAR", "MY_OTHER_VAR"]))


class TestMain(unittest.TestCase):

    def setUp(self):
        os.environ = {}

    app_conf = """
    my_var = ${?MY_VAR}
    """
    app_conf_with_invalid_hocon_name = """
    myvar = ${?MY_VAR}
    """
    env_var_docs = """
    {
        "MY_VAR": {
            "description": "A var",
            "type": "string",
            "mode": "ADMIN_READABLE"
        }
    }
    """
    invalid_env_var_docs = """
    {
        "MY_VAR": {
          "description": "A var",
          "type": "not my type",
          "mode": "BUMMER"
        }
    }
    """

    def test_hocon_name_mismatch(self):
        with tempfile.NamedTemporaryFile(
                mode='w') as appconf, tempfile.NamedTemporaryFile(
                    mode='w') as envvar:
            appconf.write(self.app_conf_with_invalid_hocon_name)
            appconf.flush()
            envvar.write(self.env_var_docs)
            envvar.flush()
            os.environ["APPLICATION_CONF_PATH"] = appconf.name
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name

            stderr = io.StringIO()
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                main()
            self.assertTrue(
                "Admin-accessible vars must have a HOCON name matching" in
                stderr.getvalue())

    def test_invalid_env_var_docs_file(self):
        with tempfile.NamedTemporaryFile(
                mode='w') as appconf, tempfile.NamedTemporaryFile(
                    mode='w') as envvar:
            appconf.write(self.app_conf)
            appconf.flush()
            envvar.write(self.invalid_env_var_docs)
            envvar.flush()
            os.environ["APPLICATION_CONF_PATH"] = appconf.name
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name

            stderr = io.StringIO()
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                main()
            self.assertTrue("is invalid:" in stderr.getvalue())

    def test_under_documented(self):
        with tempfile.NamedTemporaryFile(
                mode='w') as appconf, tempfile.NamedTemporaryFile(
                    mode='w') as envvar:
            appconf.write(self.app_conf)
            appconf.flush()
            envvar.write("{}")
            envvar.flush()
            os.environ["APPLICATION_CONF_PATH"] = appconf.name
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name

            stderr = io.StringIO()
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                main()
            self.assertTrue(
                "The following vars are not documented" in stderr.getvalue())

    def test_over_documented(self):
        with tempfile.NamedTemporaryFile(
                mode='w') as appconf, tempfile.NamedTemporaryFile(
                    mode='w') as envvar:
            appconf.write("")
            appconf.flush()
            envvar.write(self.env_var_docs)
            envvar.flush()
            os.environ["APPLICATION_CONF_PATH"] = appconf.name
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name

            stderr = io.StringIO()
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                main()
            self.assertRegex(
                stderr.getvalue(),
                r"The following vars are documented in .* but not referenced")

    def test_documented(self):
        with tempfile.NamedTemporaryFile(
                mode='w') as appconf, tempfile.NamedTemporaryFile(
                    mode='w') as envvar:
            appconf.write(self.app_conf)
            appconf.flush()
            envvar.write(self.env_var_docs)
            envvar.flush()
            os.environ["APPLICATION_CONF_PATH"] = appconf.name
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name

            # Tests that main runs without exiting.
            main()


if __name__ == "__main__":
    unittest.main()
