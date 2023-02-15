from env_var_docs.visitor import NodeInfo
from run_regex_tests import _run_test, validate_env_variables, main
import contextlib
import io
import os
import tempfile
import unittest


class TestValidateEnvVariables(unittest.TestCase):

    def setUp(self):
        os.environ = {}

    def test_no_env_vars(self):
        stderr = io.StringIO()
        with self.assertRaises(SystemExit), contextlib.redirect_stderr(stderr):
            validate_env_variables()
        self.assertTrue(
            "ENV_VAR_DOCS_PATH must be present in the environment variables" in
            stderr.getvalue())

    def test_path_returned(self):
        with tempfile.NamedTemporaryFile() as envvar:
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            got = validate_env_variables()
            self.assertEqual(got, envvar.name)


class TestRunTest(unittest.TestCase):

    def test_assertions(self):
        with self.assertRaises(AssertionError):
            _run_test(NodeInfo(level=0, type="group", name="", details=None))
            _run_test(NodeInfo(level=0, type="variable", name="", details=None))
            _run_test(
                NodeInfo(
                    level=0,
                    type="variable",
                    name="",
                    details={"type": "string"}))

    def test_invalid_regex(self):
        got = _run_test(
            NodeInfo(
                level=0,
                type="variable",
                name="TEST",
                details={
                    "regex": "(",
                    "regex_tests": [{
                        "val": "test",
                        "shouldMatch": True
                    }]
                }))
        self.assertNotEqual(got.regex_error, "")
        self.assertFalse(got.failing_matches)

    def test_failing_match(self):
        got = _run_test(
            NodeInfo(
                level=0,
                type="variable",
                name="TEST",
                details={
                    "regex": ".*",
                    "regex_tests": [{
                        "val": "test",
                        "shouldMatch": False
                    }]
                }))
        self.assertEqual(got.regex_error, "")
        self.assertTrue(got.failing_matches)

    def test_passing_match(self):
        got = _run_test(
            NodeInfo(
                level=0,
                type="variable",
                name="TEST",
                details={
                    "regex": ".*",
                    "regex_tests": [{
                        "val": "test",
                        "shouldMatch": True
                    }]
                }))
        self.assertEqual(got.regex_error, "")
        self.assertFalse(got.failing_matches)

    def test_some_passing_some_failing_match(self):
        got = _run_test(
            NodeInfo(
                level=0,
                type="variable",
                name="TEST",
                details={
                    "regex":
                        ".*",
                    "regex_tests":
                        [
                            {
                                "val": "test",
                                "shouldMatch": True
                            }, {
                                "val": "test",
                                "shouldMatch": False
                            }
                        ]
                }))
        self.assertEqual(got.regex_error, "")
        self.assertTrue(got.failing_matches)


class TestMain(unittest.TestCase):

    def setUp(self):
        os.environ = {}

    def test_failing_tests(self):
        docs = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "regex": ".*",
                "regex_tests": [
                    { "val": "will you be my match?", "shouldMatch": false }
                ]
            }
        }
        """
        with tempfile.NamedTemporaryFile(mode='w') as envvar:
            envvar.write(docs)
            envvar.flush()
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            os.environ["LOCAL_OUTPUT"] = True

            stderr = io.StringIO()
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                main()
            self.assertTrue("Test failures" in stderr.getvalue())

    def test_no_failing_tests(self):
        docs = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "regex": ".*",
                "regex_tests": [
                    { "val": "will you be my match?", "shouldMatch": true }
                ]
            }
        }
        """
        with tempfile.NamedTemporaryFile(mode='w') as envvar:
            envvar.write(docs)
            envvar.flush()
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            os.environ["LOCAL_OUTPUT"] = True

            # Test that main does not exit.
            main()


if __name__ == "__main__":
    unittest.main()
