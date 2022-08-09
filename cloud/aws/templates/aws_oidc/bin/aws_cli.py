import subprocess
import json
from typing import Dict
from typing import List

from cloud.shared.bin.lib.config_loader import ConfigLoader


class AwsCli:
    """Wrapper class that encapsulates calls to AWS CLI."""

    def __init__(self, config: ConfigLoader):
        self.config: ConfigLoader = config

    def get_secret_value(self, secret_name: str) -> str:
        res = self._call_cli(
            [
                'secretsmanager', 'get-secret-value',
                f'--secret-id={secret_name}'
            ])
        return res['SecretString'].strip()

    def set_secret_value(self, secret_name: str, new_value: str) -> None:
        self._call_cli(
            [
                'secretsmanager', 'update-secret', f'--secret-id={secret_name}',
                f'--secret-string={new_value}'
            ])

    def _call_cli(self, args: List[str]) -> Dict:
        out = subprocess.check_output(
            args=['aws', '--output=json', f'--region={self.config.aws_region}']
            + args)
        return json.loads(out.decode('ascii'))
