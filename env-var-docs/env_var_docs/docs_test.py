import io
import os
import visitor
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

        nodes = 0

        def count_nodes(_):
            nonlocal nodes
            nodes += 1

        errors = visitor.visit(io.StringIO(example), count_nodes)
        self.assertEqual(errors, [])
        self.assertEqual(nodes, 6)


if __name__ == "__main__":
    unittest.main()
