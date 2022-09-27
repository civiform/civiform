#!/usr/bin/env python3

import re
import sys

matcher = re.compile(r'([a-z \t]*[ \t]{0,1}class [a-zA-Z0-9]* \{)\n')

def find_class_identifiers(contents):
  return [m.strip() for m in matcher.findall(contents)]

def is_final_violation(class_identifier_line):
  words_before_class = class_identifier_line[:class_identifier_line.find(' class')]
  return 'final' not in words_before_class and 'abstract' not in words_before_class

def main(file_name):
  with open(file_name, 'r') as f:
    contents = f.read()
    
    for class_identifier in find_class_identifiers(contents):
      if is_final_violation(class_identifier):
        print(f'[{file_name}]: Found violation for line "{class_identifier}"')

if __name__ == '__main__':
  main(sys.argv[1])