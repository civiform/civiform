from env_var_docs.parser import ParseError, NodeParseError
from env_var_docs.errors_formatter import format
import unittest


class TestFormat(unittest.TestCase):

    def test_no_errors(self):
        got = format([])
        self.assertEqual(got, "")

    def test_errors(self):
        got = format(
            [
                NodeParseError(
                    path="file.a",
                    group_errors=[
                        ParseError(
                            path="file.a", msg="group field is required")
                    ],
                    variable_errors=[
                        ParseError(
                            path="file.a", msg="variable field is required")
                    ]),
                NodeParseError(
                    path="file.b",
                    group_errors=[
                        ParseError(
                            path="file.b.c", msg="field has unsupported value")
                    ],
                    variable_errors=[
                        ParseError(path="file.b.d", msg="surprise!"),
                        ParseError(
                            path="file.b.d", msg="Are errors so surprising?")
                    ]),
            ])
        self.assertEqual(
            got, """file.a:
\tErrors from parsing as a Variable:
\t\tfile.a: variable field is required
\tErrors from parsing as a Group:
\t\tfile.a: group field is required
file.b:
\tErrors from parsing as a Variable:
\t\tfile.b.d: surprise!
\t\tfile.b.d: Are errors so surprising?
\tErrors from parsing as a Group:
\t\tfile.b.c: field has unsupported value
""")


if __name__ == "__main__":
    unittest.main()
