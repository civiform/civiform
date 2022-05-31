import subprocess
import os

class Check():
    remedy = None

    def name(self):
        raise NotImplementedError

    def is_ok(self):
        raise NotImplementedError

    def remedy_linux(self):
        if self.remedy == None:
            raise NotImplementedError

        return self.remedy

    def remedy_mac(self):
        if self.remedy == None:
            raise NotImplementedError

        return self.remedy

    def should_skip(self):
        return False

    def _check_command_succeeds(self, args):
        try:
            return subprocess.run(args, capture_output=True).returncode == 0
        except FileNotFoundError:
            return False

    def _get_command_output(self, args):
        try:
            process = subprocess.run(args, capture_output=True)
        except FileNotFoundError:
            return ''

        return process.stdout.decode('ascii') if process else ''

    def _get_cloud_provider(self):
        return os.environ.get('CIVIFORM_CLOUD_PROVIDER', "").lower()

    def _semver_string_to_int_array(self, semver_string):
        return [int(x) for x in semver_string.split(".")]

    def version_at_least(self, min_version, test_version):
        min_version, test_version = map(self._semver_string_to_int_array, [min_version, test_version])

        for i, min_int in enumerate(min_version):
            # Treat missing entries in shorter array as 0. e.g. "2" is
            # equivalent to "2.0.0"
            test_int = test_version[i] if i < len(test_version) else 0
            if min_int > test_int:
                return False
            if min_int < test_int:
                return True

        return True
