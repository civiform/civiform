/* ensure that the image registry bucket is created and push latest image to image to it .
Interim solution. You should not have to run terraform apply to cut a release ideally. */
# resource "null_resource" "docker-registry" {
#  provisi "local-exec" {
#   command = <<EOF
#    gcloud components install docker-credential-gcr && \
#    docker-credential-gcr configure-docker && \
#    docker pull docker.io/civiform/${var.civiform_image_name} && \
#    docker build -t gcr.io/${var.project}/civiform:latest - && \
#    docker push gcr.io/${var.project}/civiform:latest
#   EOF
#  }
# }

resource "google_cloud_run_service" "civiform_application_run_service" {
  name     = "cloudrun-civiform"
  location = var.region

  template {
    spec {
      containers {
        image = "gcr.io/${var.project_id}/${var.civiform_image_name}"
        resources {
            requests = {
                "cpu"    = "2"
                "memory" = "4000Mi"
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
        // CloudSql JDBC connector needs to use HikariDataSource to establish a jdbc connection (https://github.com/GoogleCloudPlatform/java-docs-samples/blob/HEAD/cloud-sql/postgres/servlet/src/main/java/com/example/cloudsql/ConnectionPoolContextListener.java)
        env {
          name = "DB_CONNECTION_STRING"
          value = "/cloudsql/${var.db_connection_name}"
        }
        env {
          name = "DB_PASSWORD"
      value_from {
            secret_key_ref {
              name = var.db_secret_id
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
      "run.googleapis.com/ingress" = "internal-and-cloud-load-balancing"
    } 
  }
  autogenerate_revision_name = true
}


