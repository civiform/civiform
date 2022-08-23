"""
This module contains names of AWS resources that are created by the Terraform
and which also referenced in python scripts. Each resource should contain
a comment with a link pointing to the Terraform file that defines it as
at the moment we don't have a better way to synchronize them.

Resource names use pattern {app_prefix}-{name} and this file contains only the
second part, name.
"""

# Defined in cloud/aws/templates/aws_oidc/secrets.tf
ADFS_CLIENT_ID = 'civiform_adfs_client_id'
ADFS_SECRET = 'civiform_adfs_secret'
APPLICANT_OIDC_CLIENT_ID = 'civiform_applicant_oidc_client_id'
APPLICANT_OIDC_CLIENT_SECRET = 'civiform_applicant_oidc_client_secret'
POSTGRES_PASSWORD = 'civiform_postgres_password'

# Defined in cloud/aws/templates/aws_oidc/main.tf
DATABASE = 'civiform-db'

# Defined by fargate modules in cloud/aws/templates/aws_oidc/app.tf
FARGATE_SERVICE = 'civiform-service'
LOAD_BALANCER = 'civiform-lb'
CLUSTER = 'civiform'

# Defined in cloud/aws/modules/setup/backend_storage.tf
S3_TERRAFORM_STATE_BUCKET = 'civiform-backendstate'
S3_TERRAFORM_LOCK_TABLE = 'civiform-locktable'
