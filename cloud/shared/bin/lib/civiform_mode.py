import os


def is_test():
    return os.getenv('CIVIFORM_MODE') == 'test'


def is_dev():
    return os.getenv('CIVIFORM_MODE') == 'dev'
