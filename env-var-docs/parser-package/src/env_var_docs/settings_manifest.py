"""Code generation for server/app/services/settings/SettingsManifest.java

Generates a settings manifest in Java for use in the CiviForm server based
on the contents of env_var_docs.json
"""

import dataclasses
import os
import string
import sys
import typing
from io import StringIO

# Needed for <3.9
# Needed for <3.10
from typing import Dict, List, Tuple, Union

import env_var_docs.errors_formatter
from env_var_docs.parser import Group, Mode, Node, NodeParseError, Variable, visit
from jinja2 import Environment, PackageLoader, select_autoescape


@dataclasses.dataclass
class ParsedGroup:
    group_name: str
    group_description: str
    sub_groups: List["ParsedGroup"] = dataclasses.field(default_factory=list)
    variables: Dict[str, Variable] = dataclasses.field(default_factory=dict)


@dataclasses.dataclass
class Config:
    docs_path: str
    local_output: bool


def error_exit(msg):
    """Logs a message and exits"""
    sys.stderr.write(msg + "\n")
    exit(1)


def make_config() -> Config:
    if "ENV_VAR_DOCS_PATH" not in os.environ:
        error_exit(
            "ENV_VAR_DOCS_PATH must be present in the environment variables")

    docs = os.environ["ENV_VAR_DOCS_PATH"]
    if not os.path.isfile(docs):
        error_exit(f"'{docs}' does not point to a file")

    local = os.environ.get("LOCAL_OUTPUT", "false") == "true"

    return Config(docs, local)


def main():
    config = make_config()
    with open(config.docs_path) as docs_file:
        manifest, parse_errors = generate_manifest(docs_file)

        if parse_errors:
            msg = f"{config.docs_path} is invalid:\n{env_var_docs.errors_formatter.format(parse_errors)}"
            error_exit(msg)

    if config.local_output:
        print(manifest)
        exit()

    with open("server/app/services/settings/SettingsManifest.java", "w",
              encoding="utf-8") as manifest_file:
        manifest_file.write(manifest)


def render_sections(root_group: ParsedGroup) -> str:
    out = StringIO()

    out.write("ImmutableMap.<String, SettingsSection>builder()")

    groups = "".join(
        [
            f'.put("{group.group_name}", {render_group(group)})'
            for group in root_group.sub_groups
        ])
    if groups:
        out.write(groups)

    if root_group.variables:
        group_for_top_level_vars = ParsedGroup(
            "Miscellaneous", "Top level vars", [], root_group.variables)
        out.write(
            f'.put("Miscellaneous", {render_group(group_for_top_level_vars)})')

    out.write(".build()")

    return out.getvalue()


def render_group(group: ParsedGroup) -> str:
    sub_groups = ", ".join(
        [render_group(sub_group) for sub_group in group.sub_groups])
    variables = ", ".join(
        [
            render_variable(name, variable)
            for name, variable in group.variables.items()
        ])
    return f'SettingsSection.create("{group.group_name}", "{_escape_double_quotes(group.group_description)}", ImmutableList.of({sub_groups}), ImmutableList.of({variables}))'


def render_variable(name: str, variable: Variable) -> str:
    is_required = str(variable.required).lower()
    setting_type = _get_java_setting_type(variable)
    setting_mode = str(variable.mode).replace("Mode.", "")

    if setting_type == "ENUM" and variable.values:
        allowable_values = ", ".join([f'"{val}"' for val in variable.values])
        return f'SettingDescription.create("{name}", "{_escape_double_quotes(variable.description)}", /* isRequired= */ {is_required}, SettingType.{setting_type}, SettingMode.{setting_mode}, ImmutableList.of({allowable_values}))'

    if setting_type == "STRING" and variable.regex:
        return f'SettingDescription.create("{name}", "{_escape_double_quotes(variable.description)}", /* isRequired= */ {is_required}, SettingType.{setting_type}, SettingMode.{setting_mode}, Pattern.compile("{variable.regex}"))'

    return f'SettingDescription.create("{name}", "{_escape_double_quotes(variable.description)}", /* isRequired= */ {is_required}, SettingType.{setting_type}, SettingMode.{setting_mode})'


def _get_java_setting_type(variable: Variable) -> str:
    # yapf cannot handle match/case statements so we use if/else instead
    # https://github.com/google/yapf/issues/1045
    if variable.type == "string":
        if variable.values:
            return "ENUM"
        else:
            return "STRING"
    elif variable.type == "bool":
        return "BOOLEAN"
    elif variable.type == "int":
        return "INT"
    elif variable.type == "index-list":
        return "LIST_OF_STRINGS"
    else:
        raise ValueError(f"Unrecognized variable type: {variable.type}")


def _escape_double_quotes(string: str) -> str:
    return string.replace('"', '\\"')


class GetterMethodSpec:
    name: str
    variable: Variable

    def __init__(self, name: str, variable: Variable):
        self.name = name
        self.variable = variable

    def method_name(self):
        return string.capwords(self.name, "_").replace("_", "")

    def variable_name(self):
        return self.name

    def doc(self):
        return self.variable.description

    def internal_getter(self):
        # yapf cannot handle match/case statements so we use if/else instead
        # https://github.com/google/yapf/issues/1045
        if self.variable.type == "string":
            return "getString"
        elif self.variable.type == "bool":
            return "getBool"
        elif self.variable.type == "int":
            return "getInt"
        elif self.variable.type == "index-list":
            return "getListOfStrings"

    def return_type(self):
        # yapf cannot handle match/case statements so we use if/else instead
        # https://github.com/google/yapf/issues/1045
        if self.variable.type == "string":
            return "Optional<String>"
        elif self.variable.type == "bool":
            return "boolean"
        elif self.variable.type == "int":
            return "Optional<Integer>"
        elif self.variable.type == "index-list":
            return "Optional<ImmutableList<String>>"


def generate_manifest(
    docs_file: typing.TextIO,) -> Tuple[Union[str, None], List[NodeParseError]]:
    root_group = ParsedGroup("Miscellaneous", "Miscellaneous")
    docs: Dict[str, Union[ParsedGroup, Variable]] = {"file": root_group}
    getter_method_specs: List[GetterMethodSpec] = []

    def visitor(node: Node):
        nonlocal docs

        if isinstance(node.details, Group):
            group = ParsedGroup(node.name, node.details.group_description)
            parent = typing.cast(ParsedGroup, docs[node.json_path])
            parent.sub_groups.append(group)
            docs[f"{node.json_path}.{node.name}"] = group
        else:
            parent = typing.cast(ParsedGroup, docs[node.json_path])
            getter_method_specs.append(
                GetterMethodSpec(node.name, node.details))
            parent.variables[node.name] = node.details

    errors = visit(docs_file, visitor)

    if errors:
        return None, errors

    sections = render_sections(root_group)

    env = Environment(
        loader=PackageLoader("settings_manifest"),
        autoescape=select_autoescape())

    template = env.get_template("SettingsManifest.java.jinja")

    return (
        template.render(
            sections=sections,
            getter_method_specs=getter_method_specs,
            writeable_mode=Mode.ADMIN_WRITEABLE,
        ),
        [],
    )


if __name__ == "__main__":
    main()
