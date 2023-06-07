import io
import os
from env_var_docs.parser import Mode, Variable
from env_var_docs.settings_manifest import ParsedGroup, render_sections
import unittest


class TestGenerateSettingsManifest(unittest.TestCase):

    def test_render_sections(self):
        self.maxDiff = None

        sub_groups = [
            ParsedGroup(
                "Test section", "Fake section for testing.", [
                    ParsedGroup(
                        "Test subsection", "Fake subsection for testing", [], {
                            "SUBSECTION_VARIABLE":
                                Variable(
                                    description=
                                    "Fake subsection variable for testing",
                                    type="string",
                                    required=True,
                                    values=None,
                                    regex=None,
                                    regex_tests=None,
                                    mode=Mode.HIDDEN)
                        })
                ])
        ]
        root_group = ParsedGroup(
            "ROOT", "ROOT", sub_groups, {
                "STRING_VARIABLE":
                    Variable(
                        description="Fake string variable for testing",
                        type="string",
                        required=True,
                        values=None,
                        regex=None,
                        regex_tests=None,
                        mode=Mode.ADMIN_READABLE)
            })

        result = render_sections(root_group)

        self.assertEqual(
            result,
            """ImmutableMap.of("Test section", SettingsSection.create("Test section", "Fake section for testing.",\
 ImmutableList.of(SettingsSection.create("Test subsection", "Fake subsection for testing", ImmutableList.of(), ImmutableList.of(SettingDescription.create("SUBSECTION_VARIABLE", "Fake subsection variable for testing", SettingType.STRING, SettingMode.HIDDEN)))),\
 ImmutableList.of()), "ROOT", SettingsSection.create("ROOT", "Top level vars", ImmutableList.of(), ImmutableList.of(SettingDescription.create("STRING_VARIABLE", "Fake string variable for testing", SettingType.STRING, SettingMode.ADMIN_READABLE))))"""
        )
