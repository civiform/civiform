import io
import os
import parser
import unittest


class TestDocsExample(unittest.TestCase):

    def test_example(self):
        example = ""
        with open(f"{os.path.dirname(__file__)}/README.md") as readme:
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

        def note_node(info):
            node_names.append(info.name)

        errors = parser.visit(io.StringIO(example), note_node)
        self.assertEqual(errors, [])
        self.assertEqual(
            node_names, [
                "Branding", "TITLE", "LOGO_URL", "SOME_NUMBER",
                "CLOUD_PROVIDER", "LANGUAGES"
            ])


if __name__ == "__main__":
    unittest.main()
