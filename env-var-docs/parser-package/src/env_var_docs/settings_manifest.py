import dataclasses
from jinja2 import Environment, PackageLoader, select_autoescape
from env_var_docs.parser import Variable, Node, Group, NodeParseError, visit
import env_var_docs.errors_formatter
import typing
import os
import sys
from io import StringIO


@dataclasses.dataclass
class ParsedGroup:
    group_name: str
    group_description: str
    sub_groups: list['ParsedGroup'] = dataclasses.field(default_factory=list)
    variables: dict[str, Variable] = dataclasses.field(default_factory=dict)


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

    local = (os.environ.get("LOCAL_OUTPUT", "false") == "true")

    return Config(docs, local)


def main():
    config = make_config()
    with open(config.docs_path) as docs_file:
        manifest, parse_errors = generate_manifest(docs_file)

        if len(parse_errors) != 0:
            msg = f"{config.docs_path} is invalid:\n"
            msg += env_var_docs.errors_formatter.format(parse_errors)
            error_exit(msg)

    if config.local_output:
        print(manifest)
        exit()

    with open("server/app/services/settings/SettingsManifest.java", "w",
              encoding="utf-8") as manifest_file:
        manifest_file.write(manifest)


def render_sections(root_group: ParsedGroup) -> str:
    out = StringIO()

    out.write("ImmutableMap.of(")

    groups = ", ".join(
        [
            f'"{group.group_name}", {render_group(group)}'
            for group in root_group.sub_groups
        ])
    if groups != "":
        out.write(groups)

    if root_group.variables:
        group_for_top_level_vars = ParsedGroup(
            "ROOT", "Top level vars", [], root_group.variables)
        out.write(f', "ROOT", {render_group(group_for_top_level_vars)}')

    out.write(")")

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
    setting_type = _get_java_setting_type(variable)
    return f'SettingDescription.create("{name}", "{_escape_double_quotes(variable.description)}", SettingType.{setting_type})'


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


def generate_manifest(
        docs_file: typing.TextIO) -> tuple[str | None, list[NodeParseError]]:
    root_group = ParsedGroup("ROOT", "ROOT")
    docs: dict[str, ParsedGroup | Variable] = {"file": root_group}

    def visitor(node: Node):
        nonlocal docs

        if isinstance(node.details, Group):
            group = ParsedGroup(node.name, node.details.group_description)
            parent = typing.cast(ParsedGroup, docs[node.json_path])
            parent.sub_groups.append(group)
            docs[node.json_path + "." + node.name] = group
        else:
            parent = typing.cast(ParsedGroup, docs[node.json_path])
            parent.variables[node.name] = node.details

    errors = visit(docs_file, visitor)

    if len(errors) != 0:
        return None, errors

    sections = render_sections(root_group)

    env = Environment(
        loader=PackageLoader("settings_manifest"),
        autoescape=select_autoescape())

    template = env.get_template("SettingsManifest.java.jinja")

    return template.render(sections=sections), []


if __name__ == "__main__":
    main()
