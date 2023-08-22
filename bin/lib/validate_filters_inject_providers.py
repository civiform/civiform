#! /usr/bin/env python3

import re
import sys

CLASS_NAME_MATCHER = re.compile(r'(?:[A-Z][a-z]*)+')
SERVICE_AND_REPOSITORY_IMPORT_MATCHER = re.compile(
    r'^import (?:services|repository)(?:\.\w+)*\.(\w+);$', re.MULTILINE)


def get_class_name(param_declaration):
    return CLASS_NAME_MATCHER.findall(param_declaration)[0]


def get_injected_class_names(file_name, class_contents):
    class_name = file_name.split("/")[-1].replace(".java", "")
    param_list_matcher = re.compile(
        f'{class_name}\(([\s|\S][^\)]+)\)', re.MULTILINE)
    param_list_match_results = param_list_matcher.findall(class_contents)

    if not param_list_match_results:
        return []

    param_list = [s.strip() for s in param_list_match_results[0].split(",")]
    return [
        get_class_name(param_declaration) for param_declaration in param_list
    ]


def main(file_names):
    violations = []

    for file_name in file_names:
        with open(file_name, 'r') as f:
            class_contents = f.read()
            service_and_repository_class_names = SERVICE_AND_REPOSITORY_IMPORT_MATCHER.findall(
                class_contents)
            injected_class_names = get_injected_class_names(
                file_name, class_contents)

            for injected_class_name in injected_class_names:
                if injected_class_name in service_and_repository_class_names:
                    violations.append(f'[{file_name}]: "{injected_class_name}"')

    if violations:
        sys.stdout.write(
            '''
*******************************************************************************
The following filter class constructors directly inject dependencies which may
attempt to access the database at instance initialization. This can cause the
prod-mode server to crash loop (see https://github.com/civiform/civiform/pull/5376).

Instead, inject a Provider<> of the dependency.
*******************************************************************************
''')
        sys.stdout.write('\n'.join(violations))
        sys.stdout.write('\n\n')
        sys.exit(1)


if __name__ == '__main__':
    # Either parses a set of file names passed as arguments or reads them directly from stdin.
    file_names = []
    if len(sys.argv) > 1:
        file_names = sys.argv[1:]
    else:
        file_names = [
            file_name for file_name in sys.stdin.read().split('\n') if file_name
        ]

    main(file_names)
