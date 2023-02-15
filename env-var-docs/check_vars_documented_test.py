from check_vars_documented import validate_env_variables, vars_from_application_conf, vars_from_docs, main
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
                validate_env_variables()
            self.assertTrue(
                "APPLICATION_CONF_PATH must be present in the environment variables"
                in stderr.getvalue())

    def test_no_env_var(self):
        stderr = io.StringIO()
        with tempfile.NamedTemporaryFile() as tf, contextlib.redirect_stderr(
                stderr):
            os.environ["APPLICATION_CONF_PATH"] = tf.name
            with self.assertRaises(SystemExit):
                validate_env_variables()
            self.assertTrue(
                "ENV_VAR_DOCS_PATH must be present in the environment variables"
                in stderr.getvalue())

    def test_config_returned(self):
        with tempfile.NamedTemporaryFile(
        ) as appconf, tempfile.NamedTemporaryFile() as envvar:
            os.environ["APPLICATION_CONF_PATH"] = appconf.name
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            got = validate_env_variables()
            self.assertEqual(got.app_conf_path, appconf.name)
            self.assertEqual(got.docs_path, envvar.name)


class TestVarsFromAppConf(unittest.TestCase):
    app_conf_no_vars = """
    #mykey = ${some.value}
    # mykey = ${?JAVA_HOME}
    akka {
      #log-config-on-start = ${?LOG_CONFIG_ON_START}
      #log-config-on-start = true
      logger-startup-timeout = 30s
    }
    """
    app_conf_some_vars = app_conf_no_vars + """
    mykey = ${?MY_VAR} # Some var
    myotherkey = ${? MY_OTHER_VAR   }
    """

    def test_no_vars(self):
        with io.StringIO(self.app_conf_no_vars) as f:
            got = vars_from_application_conf(f)
            self.assertSetEqual(got, set())

    def test_some_vars(self):
        with io.StringIO(self.app_conf_some_vars) as f:
            got = vars_from_application_conf(f)
            self.assertSetEqual(got, set(["MY_VAR", "MY_OTHER_VAR"]))


class TestVarsFromDocs(unittest.TestCase):
    env_var_docs = """
    {
        "MY_VAR": {
            "description": "A var",
            "type": "string"
        },
        "A group": {
            "group-description": "Some group",
            "members": {
                "MY_OTHER_VAR": {
                    "description": "Another var",
                    "type": "string"
                }
            }
        }
    }
    """

    def test_no_vars(self):
        with io.StringIO("{}") as f:
            got = vars_from_docs(f)
            self.assertSetEqual(got, set())

    def test_some_vars(self):
        with io.StringIO(self.env_var_docs) as f:
            got = vars_from_docs(f)
            self.assertSetEqual(got, set(["MY_VAR", "MY_OTHER_VAR"]))


class TestMain(unittest.TestCase):

    def setUp(self):
        os.environ = {}

    app_conf = """
    mykey = ${?MY_VAR}
    """
    env_var_docs = """
    {
        "MY_VAR": {
            "description": "A var",
            "type": "string"
        }
    }
    """

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
            self.assertTrue(
                "The following vars are documented but not referenced" in
                stderr.getvalue())

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
