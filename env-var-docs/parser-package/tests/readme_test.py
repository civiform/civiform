import io
import os
import env_var_docs.parser
import unittest


class TestReadmeExample(unittest.TestCase):
    """Tests the example environment variable documentation file in
    parser-package/README.md.
    """

    def test_example(self):
        example = ""
        with open(f"{os.path.dirname(__file__)}/../README.md") as readme:
            copying = False
            for line in readme:
                if line == "```env-var-docs-file-example\n":
                    copying = True
                    continue
                if line == "```\n":
                    copying = False
                    continue
                if copying:
                    example += line

        node_names = []

        def note_node(node):
            node_names.append(node.name)

        errors = env_var_docs.parser.visit(io.StringIO(example), note_node)
        self.assertEqual(errors, [])
        self.assertEqual(
            node_names, [
                "Branding", "TITLE", "LOGO_URL", "SOME_NUMBER",
                "CLOUD_PROVIDER", "LANGUAGES"
            ])


if __name__ == "__main__":
    unittest.main()
