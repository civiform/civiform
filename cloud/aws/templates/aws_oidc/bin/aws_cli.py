import subprocess
import json
from typing import Dict
from typing import List

from cloud.shared.bin.lib.config_loader import ConfigLoader


class AwsCli:
    """Wrapper class that encapsulates calls to AWS CLI."""

    def __init__(self, config: ConfigLoader):
        self.config: ConfigLoader = config

    def is_secret_empty(self, secret_name: str) -> bool:
        res = self._call_cli(
            [
                'secretsmanager', 'get-secret-value',
                f'--secret-id={secret_name}'
            ])
        return res['SecretString'].strip() == ''

    def set_secret_value(self, secret_name: str, new_value: str) -> None:
        self._call_cli(
            [
                'secretsmanager', 'update-secret', f'--secret-id={secret_name}',
                f'--secret-string={new_value}'
            ])

    def is_db_password_default(self, secret_name: str) -> bool:
        res = self._call_cli(
            [
                'secretsmanager', 'get-secret-value',
                f'--secret-id={secret_name}'
            ])
        return res['SecretString'].startswith("default-")

    def get_current_user(self) -> str:
        return self._call_cli(['sts', 'get-caller-identity'])['UserId']

    def update_master_password_in_database(self, db_name: str, password: str):
        self._call_cli(
            [
                'rds', 'modify-db-instance',
                f'--db-instance-identifier={db_name}',
                f'--master-user-password={password}'
            ])

    def restart_ecs_service(self, cluster: str, service_name: str):
        self._call_cli(
            [
                'ecs', 'update-service', '--force-new-deployment',
                f'--service={service_name}', f'--cluster={cluster}'
            ])

    def get_url_of_secret(self, secret_name: str) -> str:
        return f'https://{self.config.aws_region}.console.aws.amazon.com/secretsmanager/secret?name={secret_name}'

    def _call_cli(self, args: List[str]) -> Dict:
        args = [
            'aws', '--output=json', f'--region={self.config.aws_region}'
        ] + args
        out = subprocess.check_output(args=args)
        return json.loads(out.decode('ascii'))
