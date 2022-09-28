#!/usr/bin/env python3

import re
import sys

matcher = re.compile(r'([a-z \t]*[ \t]{0,1}class [a-zA-Z0-9]* \{)\n')

prefix = '// NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING '

def find_class_identifiers(contents):
  return [m.strip() for m in matcher.findall(contents)]

def is_final_violation(class_identifier_line):
  words_before_class = class_identifier_line[:class_identifier_line.find(' class')]
  return 'final' not in words_before_class and 'abstract' not in words_before_class

def final_exceptions_from_contents(contents):
  allows_subclassing_lines = [content.strip() for content in contents.split('\n') if content.strip().startswith(prefix)]
  return set((l[len(prefix):] for l in allows_subclassing_lines))

def main(file_names):
  had_error = False
  for file_name in file_names:
    with open(file_name, 'r') as f:
      contents = f.read()
      exceptions = final_exceptions_from_contents(contents)
      for class_identifier in find_class_identifiers(contents):
        if is_final_violation(class_identifier):
          class_name = class_identifier.split(' ')[-2]
          if class_name in exceptions:
            exceptions = exceptions.remove(class_name)
          else:
            print(f'[{file_name}]: Found violation for line "{class_identifier}"')
            had_error = True
      if exceptions:
        print(f'[{file_name}]: Unnecessary NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING directive(s): {exceptions}')
        had_error = True
  
  if had_error:
    sys.exit(1)

if __name__ == '__main__':
  file_names = []
  if len(sys.argv) > 1:
    file_names = sys.argv[1:]
  else:
    file_names = sys.stdin.read().split('\n')

  main([file_name for file_name in file_names if file_name.strip()])