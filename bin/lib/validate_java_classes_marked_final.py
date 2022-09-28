#!/usr/bin/env python3

import re
import sys


def find_class_identifiers(contents):
    # Look for lines with a class declaration:
    #  public static final class FooBar {
    matcher = re.compile(r'([a-z \t]*[ \t]{0,1}class [a-zA-Z0-9]* \{)\n')
    return [m.strip() for m in matcher.findall(contents)]


def is_violation(class_identifier_line):
    class_token_index = class_identifier_line.find(' class')
    words_before_class_token = class_identifier_line[:class_token_index]
    return 'final' not in words_before_class_token and 'abstract' not in words_before_class_token


def extract_non_final_classes_allowlist(contents):
    # Certain classes allow subclassing and are directly instantiable. Find any directives
    # indicating this state:
    #  // NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING FieldWithLabel
    allowlist_prefix = '// NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING '
    allowlist_lines = [
        content.strip()
        for content in contents.split('\n')
        if content.strip().startswith(allowlist_prefix)
    ]
    return set((l[len(allowlist_prefix):] for l in allowlist_lines))


def main(file_names):
    violations = []
    for file_name in file_names:
        with open(file_name, 'r') as f:
            contents = f.read()
            allowlisted_classes = extract_non_final_classes_allowlist(contents)
            for class_identifier in find_class_identifiers(contents):
                if is_violation(class_identifier):
                    # Sample class identifier line:
                    #   public static final class FooBar {
                    class_name = class_identifier.split(' ')[-2]
                    if class_name in allowlisted_classes:
                        allowlisted_classes = allowlisted_classes.remove(
                            class_name)
                    else:
                        violations.append(
                            f'[{file_name}]: "{class_identifier}"')
            if allowlisted_classes:
                violations.append(
                    f'[{file_name}]: Unnecessary NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING directive(s): {allowlisted_classes}. Please remove the directive from the file.'
                )

    if violations:
        sys.stdout.write(
            '''
*******************************************************************************
The following Java class declarations were found which do not contain the
"final" keyword. Without the "final" keyword, classes are open for subclassing
by default. Please add the "final" keyword in order to make the decision to
allow subclassing explicit rathe than implicit.
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
