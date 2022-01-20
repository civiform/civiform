resource "google_compute_network" "vpc_network" {
  name                    = "vpc-network"
  description             = "VPC network for the application. Will have 2 public and 2 private subnets in 2 regions." 
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "private_subnet_1" {
  name          = "private-subnet-1"
  ip_cidr_range = "10.192.20.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
}

resource "google_compute_subnetwork" "private_subnet_2" {
  name          = "private-subnet-2"
  ip_cidr_range = "10.192.21.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
}

resource "google_compute_subnetwork" "public_subnet_1" {
  name          = "public-subnet-1"
  ip_cidr_range = "10.192.10.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
}

resource "google_compute_subnetwork" "public_subnet_2" {
  name          = "public-subnet-2"
  ip_cidr_range = "10.192.11.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
}

