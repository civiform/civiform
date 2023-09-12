from generate_markdown import make_config, generate_markdown, new_summary
import contextlib
import io
import os
import tempfile
import textwrap
import unittest


class TestMakeConfig(unittest.TestCase):

    def setUp(self):
        os.environ = {}

    def test_no_env_vars(self):
        stderr = io.StringIO()
        with self.assertRaises(SystemExit), contextlib.redirect_stderr(stderr):
            make_config()
        self.assertTrue(
            "ENV_VAR_DOCS_PATH must be present in the environment variables" in
            stderr.getvalue())

    def test_no_env_var_docs_path(self):
        with tempfile.NamedTemporaryFile() as tf:
            os.environ["LOCAL_OUTPUT"] = "true"

            stderr = io.StringIO()
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                make_config()
            self.assertTrue(
                "ENV_VAR_DOCS_PATH must be present in the environment variables"
                in stderr.getvalue())

    def test_env_var_docs_not_file(self):
        os.environ["ENV_VAR_DOCS_PATH"] = "some/file"

        stderr = io.StringIO()
        with self.assertRaises(SystemExit), contextlib.redirect_stderr(stderr):
            make_config()
        self.assertTrue(
            "'some/file' does not point to a file" in stderr.getvalue())

    def test_local_mode_config_returned(self):
        with tempfile.NamedTemporaryFile() as envvar:
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            os.environ["LOCAL_OUTPUT"] = "true"
            got = make_config()
            self.assertEqual(got.docs_path, envvar.name)
            self.assertEqual(got.local_output, True)
            self.assertEqual(got.version, "")
            self.assertEqual(got.access_token, "")
            self.assertEqual(got.repo, "")
            self.assertEqual(got.repo_path, "")

    def test_no_release_version(self):
        with tempfile.NamedTemporaryFile() as envvar:
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            os.environ["GITHUB_ACCESS_TOKEN"] = "ABCD"
            os.environ["TARGET_REPO"] = "civiform/docs"
            os.environ["TARGET_PATH"] = "/path/to/dir"

            stderr = io.StringIO()
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                make_config()
            self.assertTrue(
                "RELEASE_VERSION must be present in the environment variables"
                in stderr.getvalue())

    def test_github_mode_absolute_path(self):
        with tempfile.NamedTemporaryFile() as envvar:
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            os.environ["RELEASE_VERSION"] = "v0.0.1"
            os.environ["GITHUB_ACCESS_TOKEN"] = "ABCD"
            os.environ["TARGET_REPO"] = "civiform/docs"
            os.environ["TARGET_PATH"] = "/path/to/dir"

            stderr = io.StringIO()
            with self.assertRaises(SystemExit), contextlib.redirect_stderr(
                    stderr):
                make_config()
            self.assertTrue("must be a relative path" in stderr.getvalue())

    def test_github_mode_config_returned(self):
        with tempfile.NamedTemporaryFile() as envvar:
            os.environ["ENV_VAR_DOCS_PATH"] = envvar.name
            os.environ["RELEASE_VERSION"] = "v0.0.1"
            os.environ["GITHUB_ACCESS_TOKEN"] = "ABCD"
            os.environ["TARGET_REPO"] = "civiform/docs"
            os.environ["TARGET_PATH"] = "path/to/dir"
            got = make_config()
            self.assertEqual(got.docs_path, envvar.name)
            self.assertEqual(got.version, "v0.0.1")
            self.assertEqual(got.local_output, False)
            self.assertEqual(got.access_token, "ABCD")
            self.assertEqual(got.repo, "civiform/docs")
            self.assertEqual(got.repo_path, "path/to/dir")


class TestGenerateMarkdown(unittest.TestCase):

    def test_no_docs(self):
        with io.StringIO("{}") as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, "")

    def test_minimal_doc(self):
        input = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "mode": "ADMIN_WRITEABLE"
            }
        }
        """
        expected = """\
        # MY_VAR

        **Admin writeable**

        A var.

        - Type: string

        """
        with io.StringIO(input) as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, textwrap.dedent(expected))

    def test_secret_var_doc(self):
        input = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "mode": "SECRET"
            }
        }
        """
        expected = """\
        # MY_VAR

        **Managed secret**

        A var.

        - Type: string

        """
        with io.StringIO(input) as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, textwrap.dedent(expected))

    def test_doc_with_required(self):
        input = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "required": true,
                "mode": "HIDDEN"
            }
        }
        """
        expected = """\
        # MY_VAR

        **Server setting**

        A var. **Required**.

        - Type: string

        """
        with io.StringIO(input) as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, textwrap.dedent(expected))

    def test_doc_with_values(self):
        input = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "values": ["one", "two"],
                "mode": "HIDDEN"
            }
        }
        """
        expected = """\
        # MY_VAR

        **Server setting**

        A var.

        - Type: string
        - Allowed values:
           - `one`
           - `two`

        """
        with io.StringIO(input) as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, textwrap.dedent(expected))

    def test_doc_with_regex(self):
        input = """
        {
            "MY_VAR": {
                "description": "A var.",
                "type": "string",
                "regex": "^must_match$",
                "regex_tests": [
                    { "val": "must_match", "should_match": true },
                    { "val": "can_match", "should_match": false }
                ],
                "mode": "HIDDEN"
            }
        }
        """
        expected = """\
        # MY_VAR

        **Server setting**

        A var.

        - Type: string
        - Validation regular expression: `^must_match$`
        - Regular expression examples:
           - `must_match` should match.
           - `can_match` should not match.

        """
        with io.StringIO(input) as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, textwrap.dedent(expected))

    def test_group(self):
        input = """
        {
            "My group": {
                "group_description": "A group.",
                "members": {}
            }
        }
        """
        expected = """\
        # My group

        A group.

        """
        with io.StringIO(input) as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, textwrap.dedent(expected))

    def test_group_in_group(self):
        input = """
        {
            "My group": {
                "group_description": "A group.",
                "members": {
                    "My sub group": {
                        "group_description": "A sub group.",
                        "members": {}
                    }
                }
            }
        }
        """
        expected = """\
        # My group

        A group.

        ## My sub group

        A sub group.

        """
        with io.StringIO(input) as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, textwrap.dedent(expected))

    def test_var_in_group(self):
        input = """
        {
            "My group": {
                "group_description": "A group.",
                "members": {
                    "MY_VAR": {
                        "description": "A var.",
                        "type": "string",
                        "mode": "HIDDEN"
                    }
                }
            }
        }
        """
        expected = """\
        # My group

        A group.

        ## MY_VAR

        **Server setting**

        A var.

        - Type: string

        """
        with io.StringIO(input) as f:
            got, gotErrors = generate_markdown(f)
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, textwrap.dedent(expected))


class TestNewSummary(unittest.TestCase):

    def test_no_parent_in_list(self):
        summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
        """)
        paths = ["docs/path1"]
        got = new_summary(summary, paths)
        self.assertEqual(got, summary)

    def test_same_links(self):
        summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
        * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
          * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
          * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
        """)
        paths = [
            "docs/it-manual/sre-playbook/server-environment-variables/v1.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v2.0.0.md"
        ]
        got = new_summary(summary, paths)
        self.assertEqual(got, summary)

    def test_same_links_with_content_after(self):
        summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
        * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
          * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
          * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
        * [Link2](link2)
        """)
        paths = [
            "docs/it-manual/sre-playbook/server-environment-variables/v1.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v2.0.0.md"
        ]
        got = new_summary(summary, paths)
        self.assertEqual(got, summary)

    def test_new_links_already_sorted(self):
        old_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
        * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
          * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
        """)
        want_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
        * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
          * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
          * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
        """)
        paths = [
            "docs/it-manual/sre-playbook/server-environment-variables/v1.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v2.0.0.md"
        ]
        got = new_summary(old_summary, paths)
        self.assertEqual(got, want_summary)

    def test_new_links_not_sorted(self):
        old_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
        * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
          * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
        """)
        want_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
        * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
          * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
          * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
        """)
        paths = [
            "docs/it-manual/sre-playbook/server-environment-variables/v2.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v1.0.0.md"
        ]
        got = new_summary(old_summary, paths)
        self.assertEqual(got, want_summary)

    def test_parent_list_indented(self):
        old_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
          * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
            * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
            * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
        """)
        want_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
          * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
            * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
            * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
            * [v3.0.0](it-manual/sre-playbook/server-environment-variables/v3.0.0.md)
        """)
        paths = [
            "docs/it-manual/sre-playbook/server-environment-variables/v2.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v1.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v3.0.0.md"
        ]
        got = new_summary(old_summary, paths)
        self.assertEqual(got, want_summary)

    def test_parent_list_indented_with_content_at_same_indent_after(self):
        old_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
          * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
            * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
            * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
          * [Link](link)
            * [Sub link](link/sublink)
        """)
        want_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
          * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
            * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
            * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
            * [v3.0.0](it-manual/sre-playbook/server-environment-variables/v3.0.0.md)
          * [Link](link)
            * [Sub link](link/sublink)
        """)
        paths = [
            "docs/it-manual/sre-playbook/server-environment-variables/v2.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v1.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v3.0.0.md"
        ]
        got = new_summary(old_summary, paths)
        self.assertEqual(got, want_summary)

    def test_parent_list_indented_with_content_at_less_indent_after(self):
        old_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
          * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
            * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
            * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)

        * [Link](link)
          * [Sub link](link/sublink)
        """)
        want_summary = textwrap.dedent(
            """\
        * [Link](link)
          * [Sub link](link/sublink)
          * [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)
            * [v1.0.0](it-manual/sre-playbook/server-environment-variables/v1.0.0.md)
            * [v2.0.0](it-manual/sre-playbook/server-environment-variables/v2.0.0.md)
            * [v3.0.0](it-manual/sre-playbook/server-environment-variables/v3.0.0.md)

        * [Link](link)
          * [Sub link](link/sublink)
        """)
        paths = [
            "docs/it-manual/sre-playbook/server-environment-variables/v2.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v1.0.0.md",
            "docs/it-manual/sre-playbook/server-environment-variables/v3.0.0.md"
        ]
        got = new_summary(old_summary, paths)
        self.assertEqual(got, want_summary)


if __name__ == "__main__":
    unittest.main()
