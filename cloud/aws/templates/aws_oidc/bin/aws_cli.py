import shlex
import subprocess
import json
from typing import Dict

from cloud.shared.bin.lib.config_loader import ConfigLoader


class AwsCli:
    """Wrapper class that encapsulates calls to AWS CLI."""

    def __init__(self, config: ConfigLoader):
        self.config: ConfigLoader = config

    def is_secret_empty(self, secret_name: str) -> bool:
        res = self._call_cli(
            f'secretsmanager get-secret-value --secret-id={secret_name}')
        return res['SecretString'].strip() == ''

    def set_secret_value(self, secret_name: str, new_value: str):
        self._call_cli(
            f'secretsmanager update-secret --secret-id={secret_name} --secret-string={new_value}'
        )

    def is_db_password_default(self, secret_name: str) -> bool:
        res = self._call_cli(
            f'secretsmanager get-secret-value --secret-id={secret_name}')
        return res['SecretString'].startswith('default-')

    def get_current_user(self) -> str:
        res = self._call_cli('sts get-caller-identity')
        return res['UserId']

    def update_master_password_in_database(self, db_name: str, password: str):
        self._call_cli(
            f'rds modify-db-instance --db-instance-identifier={db_name} --master-user-password={password} '
        )

    def restart_ecs_service(self, cluster: str, service_name: str):
        self._call_cli(
            f'ecs update-service --force-new-deployment --service={service_name} --cluster={cluster}'
        )

    def get_url_of_secret(self, secret_name: str) -> str:
        return f'https://{self.config.aws_region}.console.aws.amazon.com/secretsmanager/secret?name={secret_name}'

    def get_url_of_s3_bucket(self, bucket_name: str) -> str:
        return f'https://{self.config.aws_region}.console.aws.amazon.com/s3/buckets/{bucket_name}'

    def _call_cli(self, command: str) -> Dict:
        command = f'aws --output=json --region={self.config.aws_region} ' + command
        out = subprocess.check_output(shlex.split(command))
        return json.loads(out.decode('ascii'))
