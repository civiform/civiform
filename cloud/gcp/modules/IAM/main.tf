resource "google_service_account" "application_service_account" {
  account_id   = "civiform-application"
  display_name = "Civiform Service Account"
}

// Requires db access, storage access, secret access and service account user.
data "google_iam_policy" "application_service_account_policy" {
  binding {
    role = "roles/cloudsql.client"

    members = [
      "serviceAccount:${google_service_account.application_service_account.email}",
    ]
  }

  binding {
    role = "roles/storage.objectAdmin"

    members = [
      "serviceAccount:${google_service_account.application_service_account.email}",
    ]
  }

  binding {
    role = "roles/cloudkms.viewer"

    members = [
      "serviceAccount:${google_service_account.application_service_account.email}",
    ]
  }


  binding {
    role = "roles/secretmanager.secretAccessor"

    members = [
      "serviceAccount:${google_service_account.application_service_account.email}",
    ]
  }

  binding {
    role = "roles/iam.serviceAccountUser"

    members = [
      "serviceAccount:${google_service_account.application_service_account.email}",
    ]
  }
}

resource "google_storage_bucket_iam_binding" "binding" {
 role   = "roles/storage.legacyBucketOwner"
 bucket = "artifacts.${var.project_id}.appspot.com"

 members = [
  "serviceAccount:${var.terraform_service_account_email}",
 ]
}
