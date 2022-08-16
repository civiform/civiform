"""
This module contains names of AWS resources that are created by the Terraform
and which also referenced in python scripts. Each resource should contain
a comment with a link pointing to the Terraform file that defines it as
at the moment we don't have a better way to synchronize them.

Resource names use pattern {app_prefix}-{name} and this file contains only the
second part, name.
"""

# Defined in cloud/aws/templates/aws_oidc/secrets.tf
ADFS_CLIENT_ID = 'adfs_client_id'
ADFS_SECRET = 'adfs_secret'
APPLICANT_OIDC_CLIENT_ID = 'applicant_oidc_client_id'
APPLICANT_OIDC_CLIENT_SECRET = 'applicant_oidc_client_secret'
POSTGRES_PASSWORD = 'postgres_password'

# Defined in cloud/aws/templates/aws_oidc/main.tf
DATABASE = 'civiform-db'

# Defined by fargate modules in cloud/aws/templates/aws_oidc/app.tf
FARGATE_SERVICE = 'service'
LOAD_BALANCER = 'lb'

# Defined in cloud/aws/modules/setup/backend_storage.tf
S3_TERRAFORM_STATE_BUCKET = 'backendstate'
