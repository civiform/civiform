resource "google_compute_network" "vpc_network" {
  name                    = "vpc-network"
  description             = "VPC network for the application. Will have 2 public and 2 private subnets in 2 regions."
  auto_create_subnetworks = false
}

