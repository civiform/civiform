# Unit tests for `bin/create-release`. Run manually; right now we don't
# have automation to find any kind of tests in this folder.
#
# Run with "python bin/create-release-test.py -v"

import contextlib
import importlib.machinery
import importlib.util
import io
from pathlib import Path
import unittest
from unittest import mock

loader = importlib.machinery.SourceFileLoader(
    'create_release', str(Path(__file__).with_name('create-release')))
spec = importlib.util.spec_from_loader(loader.name, loader)
create_release = importlib.util.module_from_spec(spec)
loader.exec_module(create_release)


class CreateReleaseTest(unittest.TestCase):

    # Mock out everything main() touches so nothing actually runs or gets
    # pushed. Tests just set self.branches to say where the commit lives.
    def setUp(self):
        self.shell = self.enterContext(
            mock.patch.object(create_release, 'shell_cmd'))
        self.shell.side_effect = self.shell_output
        self.get = self.enterContext(
            mock.patch.object(create_release.requests, 'get'))
        self.get.return_value.status_code = 200
        self.set_docker_tags(['SNAPSHOT-abcdef0-12345'])
        self.post = self.enterContext(
            mock.patch.object(create_release.requests, 'post'))
        self.post.return_value.status_code = 201
        self.post.return_value.json.return_value = {
            'html_url': 'https://github.com/civiform/civiform/releases/1'
        }
        self.enterContext(
            mock.patch.dict(
                create_release.os.environ, {'GH_TOKEN': 'test-token'},
                clear=True))
        self.enterContext(contextlib.redirect_stdout(io.StringIO()))
        self.stderr = self.enterContext(
            contextlib.redirect_stderr(io.StringIO()))
        self.branches = '  origin/main\n'

    # Fake a single page of Docker Hub tag results.
    def set_docker_tags(self, names):
        self.get.return_value.json.return_value = {
            'results': [{
                'name': name
            } for name in names],
            'next': None
        }

    # Fake output for the shell commands we care about. Anything else
    # (git tag, git push, docker pull/tag/push) just returns an empty string.
    def shell_output(self, command, timeout=None):
        return {
            'git config --get user.email': 'releaser@example.com',
            'docker login': 'Login Succeeded',
            'git --no-pager branch -r --contains abcdef0': self.branches,
            'git rev-parse --short abcdef0': 'abcdef0\n',
        }.get(command, '')

    # Run the script with a fixed SHA, a version, and any extra flags.
    def run_release(self, *args, version='v1.2.0'):
        with mock.patch.object(
                create_release.sys, 'argv',
            ['bin/create-release', 'abcdef0', version, *args]):
            create_release.main()

    # No --branch and the commit is on main, so this should just work.
    def test_defaults_to_main(self):
        self.run_release()

        self.post.assert_called_once()
        self.shell.assert_any_call('git push origin v1.2.0')

    # Passing --branch main with patch 0 should work just like leaving
    # --branch out.
    def test_accepts_zero_patch_on_explicit_main(self):
        self.run_release('--branch', 'main')

        self.post.assert_called_once()
        self.shell.assert_any_call('git push origin v1.2.0')

    # Main only allows patch 0, whether we pass --branch or leave it out.
    # Check we bail out before tagging or pushing anything.
    def test_rejects_positive_patch_on_main(self):
        for args in [(), ('--branch', 'main')]:
            with self.subTest(args=args):
                self.shell.reset_mock()
                self.get.reset_mock()

                with self.assertRaises(SystemExit) as error:
                    self.run_release(*args, version='v1.2.1')

                self.assertEqual(error.exception.code, 1)
                self.assertIn(
                    'Releases from main must have patch 0.',
                    self.stderr.getvalue())
                self.post.assert_not_called()
                self.assertEqual(self.get.call_count, 1)
                self.assertEqual(self.shell.call_count, 2)

    # Release branches need a patch greater than 0. Patch 0 should be
    # rejected even if the commit really is on the selected branch.
    def test_rejects_zero_patch_on_release_branch(self):
        self.branches = '  origin/release/1.2\n'

        with self.assertRaises(SystemExit) as error:
            self.run_release('--branch', 'release/1.2')

        self.assertEqual(error.exception.code, 1)
        self.assertIn(
            'Releases from release/* must have patch greater than 0.',
            self.stderr.getvalue())
        self.post.assert_not_called()
        self.assertEqual(self.get.call_count, 1)
        self.assertEqual(self.shell.call_count, 2)

    # The commit is only on the release branch, not main. Passing --branch
    # should be enough to let it through.
    def test_accepts_selected_branch_without_main(self):
        self.branches = '  origin/release/1.2\n'

        self.run_release('--branch', 'release/1.2', version='v1.2.1')

        self.post.assert_called_once()
        self.shell.assert_any_call('git push origin v1.2.1')

    # release/1.20 should not match release/1.2. Also check we bail out right
    # after the branch check and don't tag or push anything.
    def test_rejects_commit_outside_selected_branch(self):
        self.branches = '  origin/main\n  origin/release/1.20\n'

        with self.assertRaises(SystemExit) as error:
            self.run_release('--branch', 'release/1.2', version='v1.2.3')

        self.assertEqual(error.exception.code, 1)
        self.assertIn(
            'abcdef0 is not on origin/release/1.2', self.stderr.getvalue())
        self.post.assert_not_called()
        self.assertEqual(self.get.call_count, 1)
        self.assertEqual(self.shell.call_count, 3)

    # We only release from main or release/*. A feature branch should be
    # rejected even if the commit really is on it.
    def test_rejects_branch_outside_release_pattern(self):
        self.branches = '  origin/feature/foo\n'

        with self.assertRaises(SystemExit) as error:
            self.run_release('--branch', 'feature/foo')

        self.assertEqual(error.exception.code, 1)
        self.assertIn('Invalid branch: feature/foo', self.stderr.getvalue())
        self.post.assert_not_called()

    # The version must look like vMAJOR.MINOR.PATCH. Anything else should
    # be rejected before we touch the branch list, tag, or push.
    def test_rejects_malformed_version(self):
        for version in ['1.2.0', 'v1.2', 'v1.2.0-rc1', 'vx.y.z', 'v1x2x0']:
            with self.subTest(version=version):
                self.shell.reset_mock()

                with self.assertRaises(SystemExit) as error:
                    self.run_release(version=version)

                self.assertEqual(error.exception.code, 1)
                self.assertIn(
                    f'Invalid version number: {version}',
                    self.stderr.getvalue())
                self.post.assert_not_called()
                self.assertEqual(self.shell.call_count, 2)

    # git marks the current branch with a leading '*'. That marker has to
    # be stripped so the branch still matches what we're releasing from.
    def test_strips_current_branch_marker(self):
        self.branches = '* origin/main\n  origin/release/1.2\n'

        self.run_release()

        self.post.assert_called_once()
        self.shell.assert_any_call('git push origin v1.2.0')

    # A RELEASE- image built from a release branch is allowed to be released.
    def test_accepts_release_tag_on_release_branch(self):
        self.branches = '  origin/release/1.2\n'
        self.set_docker_tags(['RELEASE-abcdef0-12345-release-1.2'])

        self.run_release('--branch', 'release/1.2', version='v1.2.1')

        self.post.assert_called_once()
        self.shell.assert_any_call(
            'docker pull civiform/civiform:RELEASE-abcdef0-12345-release-1.2')
        self.shell.assert_any_call('git push origin v1.2.1')

    # A DEV- image built from a feature branch must not be released, even if
    # the commit is on the selected branch. Nothing should be tagged or pushed.
    def test_rejects_dev_tag(self):
        self.branches = '  origin/release/1.2\n'
        self.set_docker_tags(['DEV-abcdef0-12345-my-feature'])

        with self.assertRaises(SystemExit) as error:
            self.run_release('--branch', 'release/1.2', version='v1.2.1')

        self.assertEqual(error.exception.code, 1)
        self.assertIn(
            'No SNAPSHOT- or RELEASE- image tag found with short SHA: abcdef0',
            self.stderr.getvalue())
        self.post.assert_not_called()
        for call in self.shell.call_args_list:
            self.assertNotIn('git tag', call.args[0])
            self.assertNotIn('git push', call.args[0])
            self.assertNotIn('docker push', call.args[0])

    # A DEV- tag for the same SHA should be skipped in favor of a releasable
    # tag further down the list.
    def test_skips_dev_tag_when_releasable_tag_exists(self):
        self.set_docker_tags(
            ['DEV-abcdef0-12345-my-feature', 'SNAPSHOT-abcdef0-12345'])

        self.run_release()

        self.post.assert_called_once()
        self.shell.assert_any_call(
            'docker pull civiform/civiform:SNAPSHOT-abcdef0-12345')

    # No --branch means main. A commit that's only on a release branch
    # should be rejected.
    def test_default_rejects_commit_only_on_other_branch(self):
        self.branches = '  origin/release/1.2\n'

        with self.assertRaises(SystemExit) as error:
            self.run_release()

        self.assertEqual(error.exception.code, 1)
        self.assertIn('abcdef0 is not on origin/main', self.stderr.getvalue())
        self.post.assert_not_called()
        self.assertEqual(self.shell.call_count, 3)


if __name__ == '__main__':
    unittest.main()
