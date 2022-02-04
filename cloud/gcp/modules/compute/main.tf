resource "google_cloud_run_service" "civiform_application_run_service" {
  name     = "cloudrun-civiform"
  location = var.region

  template {
    spec {
      containers {
        image = "us-docker.pkg.dev/cloudrun/container/hello"
        resources {
            requests = {
                "cpu"    = "1"
                "memory" = "512Mi"
            }
            limits = {
                cpu      = "4"
                "memory" = "7182Mi"
            }
        }
        ports {
          name = "http1"
          container_port = var.http_port
        }
        env {
          name = "DB_USERNAME"
          value = var.application_service_account_email
        }
        env {
          name = "REGION"
          value = var.region
        }
        env {
          name = "GCS_BUCKET_NAME"
          value = var.bucket_name
        }
        // CloudSql JDBC connector connects a little differently(see https://cloud.google.com/sql/docs/postgres/connect-run)
        env {
          name = "DB_CONNECTION_STRING"
          value = "/cloudsql/${var.connection_name}"
        }
        env {
          name = "DB_PASSWORD"
      value_from {
            secret_key_ref {
              name = var.secret_id
              key = "latest"
            }
          }
        }
      }
        service_account_name = var.application_service_account_email
    }
    metadata {
      annotations = {
        "autoscaling.knative.dev/maxScale"      = "5"
        "autoscaling.knative.dev/minScale"      = "2"
        "run.googleapis.com/cloudsql-instances" = var.connection_name
      }
    }
  }
  metadata { 
    annotations = { 
      "run.googleapis.com/ingress"       = "internal-and-cloud-load-balancing"
    } 
  }
  autogenerate_revision_name = true
}


