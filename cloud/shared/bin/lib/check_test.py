import unittest

from check import Check

class TestCheck(unittest.TestCase):
    def test_version_greater_than(self):
        check = Check()

        self.assertTrue(check.version_greater_than("1", "2"))
        self.assertFalse(check.version_greater_than("2", "1"))
        self.assertTrue(check.version_greater_than("1.1", "1.1"))
        self.assertTrue(check.version_greater_than("1.1", "1.2"))
        self.assertTrue(check.version_greater_than("1.1", "1.1.3"))
        self.assertFalse(check.version_greater_than("1.2", "1.1.3"))
        self.assertFalse(check.version_greater_than("1.0.4", "1.1.3"))

if __name__ == '__main__':
    unittest.main()
