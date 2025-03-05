import io
import os
import unittest

from env_var_docs.parser import Mode, Variable
from env_var_docs.settings_manifest import ParsedGroup, render_sections


class TestGenerateSettingsManifest(unittest.TestCase):

    def test_render_sections(self):
        self.maxDiff = None

        sub_groups = [
            ParsedGroup(
                "Test section",
                "Fake section for testing.",
                [
                    ParsedGroup(
                        "Test subsection",
                        "Fake subsection for testing",
                        [],
                        {
                            "SUBSECTION_VARIABLE": Variable(
                                description="Fake subsection variable for testing",
                                type="string",
                                required=True,
                                values=None,
                                regex=None,
                                regex_tests=None,
                                mode=Mode.HIDDEN,
                            )
                        },
                    )
                ],
            )
        ]
        root_group = ParsedGroup(
            "Miscellaneous",
            "Miscellaneous",
            sub_groups,
            {
                "STRING_VARIABLE": Variable(
                    description="Fake string variable for testing",
                    type="string",
                    required=True,
                    values=None,
                    regex=None,
                    regex_tests=None,
                    mode=Mode.ADMIN_READABLE,
                ),
                "ENUM_VARIABLE": Variable(
                    description="Fake string variable for testing",
                    type="string",
                    required=True,
                    values=["one", "two"],
                    regex=None,
                    regex_tests=None,
                    mode=Mode.ADMIN_READABLE,
                ),
                "REGEX_VARIABLE": Variable(
                    description="Fake string variable for testing",
                    type="string",
                    required=False,
                    values=None,
                    regex="^regex$",
                    regex_tests=None,
                    mode=Mode.ADMIN_READABLE,
                ),
            },
        )

        result = render_sections(root_group)

        self.assertEqual(
            result,
            """ImmutableMap.<String, SettingsSection>builder().put("Test section", SettingsSection.create("Test section", "Fake section for testing.", \
ImmutableList.of(SettingsSection.create("Test subsection", "Fake subsection for testing", ImmutableList.of(), \
ImmutableList.of(SettingDescription.create("SUBSECTION_VARIABLE", "Fake subsection variable for testing", /* isRequired= */ true, SettingType.STRING, SettingMode.HIDDEN)))), \
ImmutableList.of())).put("Miscellaneous", SettingsSection.create("Miscellaneous", "Top level vars", ImmutableList.of(), \
ImmutableList.of(SettingDescription.create("STRING_VARIABLE", "Fake string variable for testing", /* isRequired= */ true, SettingType.STRING, SettingMode.ADMIN_READABLE), \
SettingDescription.create("ENUM_VARIABLE", "Fake string variable for testing", /* isRequired= */ true, SettingType.ENUM, SettingMode.ADMIN_READABLE, ImmutableList.of("one", "two")), \
SettingDescription.create("REGEX_VARIABLE", "Fake string variable for testing", /* isRequired= */ false, SettingType.STRING, SettingMode.ADMIN_READABLE, Pattern.compile("^regex$"))))).build()""",
        )
