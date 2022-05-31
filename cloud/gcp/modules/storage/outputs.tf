output "bucket_name" {
  value = google_storage_bucket.file_storage.name
}

output "storage_id" {
  value = google_storage_bucket.file_storage.id
}