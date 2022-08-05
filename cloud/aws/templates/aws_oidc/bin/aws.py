import subprocess
import json
from typing import Dict
from typing import List

from cloud.shared.bin.lib.config_loader import ConfigLoader


class Aws():
    """Wrapper class that encapsulates calls to AWS CLI."""

    def __init__(self, config: ConfigLoader):
        self.config: ConfigLoader = config

    def get_all_secrets(self)-> Dict:
        return self._call_cli(["secretsmanager", "list-secrets"])

    def get_secret_by_name(self, secrets: Dict, name: str) -> Dict:
        for secret in secrets["SecretList"]:
            if secret["Name"] == name:
                return secret
        raise ValueError(f"Secret with name {name} is not found. Was it successfully created?")

    def get_secret_value(self, secret: Dict) -> str:
        arn = secret["ARN"]
        res = self._call_cli(["secretsmanager", "get-secret-value", f"--secret-id={arn}"])
        return res["SecretString"].strip()

    def set_secret_value(self, secret: Dict, new_value: str) -> None:
        arn = secret['ARN']
        self._call_cli(["secretsmanager", "update-secret", f"--secret-id={arn}", f"--secret-string={new_value}"])

    def _call_cli(self, args: List[str]) -> Dict:
        out = subprocess.check_output(args = ["aws", "--output=json", f"--region={self.config.aws_region}"] + args)
        return json.loads(out.decode("ascii"))
