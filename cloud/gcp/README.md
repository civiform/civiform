# Home to CivForm Terraform code for GCP.

## Initial Setup

- Create a service account for terraform. Your user will need `PROJECT IAM ADMIN` role for this.
- Add your user for role `Service Account Token Creator` to the Service Account's IAM binding. Note that this is service account IAM binding and not user IAM binding.
- Run `gcloud auth application-default login`
- Optional: Create a gcp bucket to store state files for terraform and give terraform service account permissions to write to the bucket. see [terraform doc](https://www.terraform.io/language/settings/backends/gcs)
