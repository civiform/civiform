import io
import os
import unittest

from env_var_docs.parser import Mode, Variable
from env_var_docs.settings_manifest import GetterMethodSpec, ParsedGroup, enum_imports, render_sections


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
                            "SUBSECTION_VARIABLE":
                                Variable(
                                    description=
                                    "Fake subsection variable for testing",
                                    type="string",
                                    required=True,
                                    values=None,
                                    regex=None,
                                    regex_tests=None,
                                    enum_class=None,
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
                "STRING_VARIABLE":
                    Variable(
                        description="Fake string variable for testing",
                        type="string",
                        required=True,
                        values=None,
                        regex=None,
                        regex_tests=None,
                        enum_class=None,
                        mode=Mode.ADMIN_READABLE,
                    ),
                "ENUM_VARIABLE":
                    Variable(
                        description="Fake string variable for testing",
                        type="string",
                        required=True,
                        values=["one", "two"],
                        regex=None,
                        regex_tests=None,
                        enum_class=None,
                        mode=Mode.ADMIN_READABLE,
                    ),
                "REGEX_VARIABLE":
                    Variable(
                        description="Fake string variable for testing",
                        type="string",
                        required=False,
                        values=None,
                        regex="^regex$",
                        regex_tests=None,
                        enum_class=None,
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


class TestGetterMethodSpec(unittest.TestCase):

    def test_string_getter(self):
        var = Variable(
            description="A string var",
            type="string",
            required=False,
            values=None,
            regex=None,
            regex_tests=None,
            enum_class=None,
            mode=Mode.HIDDEN,
        )
        spec = GetterMethodSpec("MY_STRING_VAR", var)
        self.assertEqual(spec.internal_getter(), "getString")
        self.assertEqual(spec.return_type(), "Optional<String>")
        self.assertEqual(spec.enum_class_arg(), "")

    def test_enum_getter(self):
        var = Variable(
            description="An enum var",
            type="string",
            required=False,
            values=["A", "B"],
            regex=None,
            regex_tests=None,
            enum_class="some.EnumType",
            mode=Mode.HIDDEN,
        )
        spec = GetterMethodSpec("MY_ENUM_VAR", var)
        self.assertEqual(spec.internal_getter(), "getEnum")
        self.assertEqual(spec.return_type(), "Optional<EnumType>")
        self.assertEqual(spec.enum_class_arg(), ", EnumType.class")

    def test_enum_imports(self):
        var_with_enum = Variable(
            description="An enum var",
            type="string",
            required=False,
            values=["A", "B"],
            regex=None,
            regex_tests=None,
            enum_class="auth.ClientIpType",
            mode=Mode.HIDDEN,
        )
        var_without_enum = Variable(
            description="A string var",
            type="string",
            required=False,
            values=None,
            regex=None,
            regex_tests=None,
            enum_class=None,
            mode=Mode.HIDDEN,
        )
        specs = [
            GetterMethodSpec("VAR_A", var_with_enum),
            GetterMethodSpec("VAR_B", var_without_enum),
            GetterMethodSpec("VAR_C", var_with_enum),
        ]
        result = enum_imports(specs)
        self.assertEqual(result, ["auth.ClientIpType"])
