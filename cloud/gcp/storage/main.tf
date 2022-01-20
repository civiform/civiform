resource "google_kms_key_ring" "keyring" {
  name     = "civform-keyring"
  location = var.region
}

resource "google_kms_crypto_key" "storage-key" {
  name            = "cloud-storage-key"
  key_ring        = google_kms_key_ring.keyring.id
  rotation_period = "604800s" //7 days

  lifecycle {
    prevent_destroy = true
  }
}

// Need the gcs service account name beforehand.
data "google_storage_project_service_account" "gcs_account" {
}

// Allow gcs service account on key.
resource "google_kms_crypto_key_iam_binding" "kms-binding" {
  crypto_key_id = google_kms_crypto_key.storage-key.id
  role          = "roles/cloudkms.cryptoKeyEncrypterDecrypter"
  members = ["serviceAccount:${data.google_storage_project_service_account.gcs_account.email_address}"]
}


resource "google_storage_bucket" "file-storage" {
  name          = "cloud-filestore-${var.company-name}"
  location      = var.region
  force_destroy = true

  uniform_bucket_level_access = true

  versioning {
    enabled = true
  }

  encryption {
    default_kms_key_name = google_kms_crypto_key.storage-key.id
  }

  depends_on = [
    google_kms_crypto_key_iam_binding.kms-binding
  ]
}

## TODO(ktoor@google.com) : Remove user and add service accounts for GKE.
data "google_iam_policy" "storage-viewer" {
  binding {
    role = "roles/storage.legacyBucketReader"
    members = [
      "user:ktoor@google.com",
    ]
  }
}

resource "google_storage_bucket_iam_policy" "viewer-policy" {
  bucket = google_storage_bucket.file-storage.name
  policy_data = data.google_iam_policy.storage-viewer.policy_data
}

data "google_iam_policy" "storage-owner" {
  binding {
    role = "roles/storage.legacyBucketOwner"
    members = [
      "serviceAccount:civform-terraform@civiform-demo.iam.gserviceaccount.com",
    ]
  }
}

resource "google_storage_bucket_iam_policy" "owner-policy" {
  bucket = google_storage_bucket.file-storage.name
  policy_data = data.google_iam_policy.storage-owner.policy_data
}