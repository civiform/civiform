from env_var_docs.visitor import NodeInfo
from run_regex_tests import _run_test
import unittest


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


if __name__ == "__main__":
    unittest.main()
