import unittest

from check import Check

class TestCheck(unittest.TestCase):
    def test_version_at_least(self):
        check = Check()

        self.assertTrue(check.version_at_least("1", "2"))
        self.assertFalse(check.version_at_least("2", "1"))
        self.assertTrue(check.version_at_least("1.1", "1.1"))
        self.assertTrue(check.version_at_least("1.1", "1.2"))
        self.assertTrue(check.version_at_least("1.1", "1.1.3"))
        self.assertFalse(check.version_at_least("1.2", "1.1.3"))
        self.assertTrue(check.version_at_least("1.0.4", "1.1.3"))
        self.assertFalse(check.version_at_least("2.36.12", "2.36"))
        self.assertTrue(check.version_at_least("1.0.0", "1"))
        self.assertFalse(check.version_at_least("1.0.1", "1"))

if __name__ == '__main__':
    unittest.main()
