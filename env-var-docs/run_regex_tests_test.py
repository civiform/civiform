from env_var_docs.parser import Node, Variable, RegexTest
from run_regex_tests import _run_test, env_var_docs_path, main
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
            env_var_docs_path()
        self.assertTrue(
            "ENV_VAR_DOCS_PATH must be present in the environment variables" in
            stderr.getvalue())

    def test_points_to_dir(self):
        stderr = io.StringIO()
        with tempfile.TemporaryDirectory() as dir_name:
            os.environ["ENV_VAR_DOCS_PATH"] = dir_name
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                env_var_docs_path()
            self.assertTrue("does not point to a file" in stderr.getvalue())

    def test_path_returned(self):
        with tempfile.NamedTemporaryFile() as envvar:
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            got = env_var_docs_path()
            self.assertEqual(got, envvar.name)


class TestRunTest(unittest.TestCase):

    def test_invalid_regex(self):
        got = _run_test(
            "TEST_VAR", "(", [RegexTest(val="test", should_match=True)])
        self.assertNotEqual(got.regex_error, "")
        self.assertFalse(got.failing_matches)

    def test_failing_match(self):
        got = _run_test(
            "TEST_VAR", ".*", [RegexTest(val="test", should_match=False)])
        self.assertEqual(got.regex_error, "")
        self.assertTrue(got.failing_matches)

    def test_passing_match(self):
        got = _run_test(
            "TEST_VAR", ".*", [RegexTest(val="test", should_match=True)])
        self.assertEqual(got.regex_error, "")
        self.assertFalse(got.failing_matches)

    def test_some_passing_some_failing_match(self):
        got = _run_test(
            "TEST_VAR",
            ".*",
            [
                # Passing RegexTest because 'test' matches '.*'.
                RegexTest(val="test", should_match=True),
                # Failing RegexTest because 'test' matches '.*'.
                RegexTest(val="test", should_match=False)
            ])
        self.assertEqual(got.regex_error, "")
        self.assertTrue(got.failing_matches)


class TestMain(unittest.TestCase):

    def setUp(self):
        os.environ = {}

    def test_invalid_regex_test(self):
        docs = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "regex": ".*",
                "regex_tests": [
                    { "valz": "Oops", "should_match": true }
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
            self.assertIn(f"{envvar.name} is invalid:", stderr.getvalue())

    def test_failing_tests(self):
        docs = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "regex": ".*",
                "regex_tests": [
                    { "val": "will you be my match?", "should_match": false }
                ],
                "mode": "HIDDEN"
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
            self.assertIn("Test failures: ['MY_VAR']", stderr.getvalue())

    def test_no_failing_tests(self):
        docs = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "regex": ".*",
                "regex_tests": [
                    { "val": "will you be my match?", "should_match": true }
                ],
                "mode": "HIDDEN"
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

    def test_with_group(self):
        docs = """
        {
            "A group": {
                "group_description": "A group",
                "members": {
                    "MY_VAR": {
                        "description": "A var.",
                        "type": "string",
                        "regex": ".*",
                        "regex_tests": [
                            { "val": "will you be my match?", "should_match": true }
                        ],
                        "mode": "HIDDEN"
                    }
                }
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
