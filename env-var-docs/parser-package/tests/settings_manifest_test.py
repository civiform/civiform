import io
import os
from env_var_docs.settings_manifest import ParsedGroup, render_sections
import unittest

class TestGenerateSettingsManifest(unittest.TestCase):

    def test_render_sections(self):
        root_group = env_ParsedGroup("ROOT", "ROOT")

        result = render_sections(root_group)

        self.assertEqual(
            result, """ImmutableMap.of(
                         "TEST_SECTION", SettingsSection.create("Test Section", "Fake section for testing.",
                          ImmutableList.of(SettingsSection.create("Test Subsection", "Fake subsection for testing", ImmutableList.of(), ImmutableList.of(
                          SettingDescription.create("Subsection variable", "Fake subsection variable for testing", SettingType.STRING)
                         ))),
                         ImmutableList.of(
                             SettingDescription.create("String variable", "Fake string variable for testing", SettingType.STRING)
                         )
        )""")
