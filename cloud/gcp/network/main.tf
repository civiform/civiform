resource "google_compute_network" "vpc_network" {
  name                    = "vpc-network"
  description             = "VPC network for the application. Will have 2 public and 2 private subnets in 2 regions." 
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "private_subnet_nat_1" {
  name          = "private-subnet-nat-1"
  ip_cidr_range = "10.192.20.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
  private_ip_google_access = true
}

resource "google_compute_subnetwork" "private_subnet_nat_2" {
  name          = "private-subnet-nat-2"
  ip_cidr_range = "10.192.21.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
  private_ip_google_access = true
}

resource "google_compute_subnetwork" "public_subnet_vm_1" {
  name          = "public-subnet-vm-1"
  ip_cidr_range = "10.192.10.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
}

resource "google_compute_subnetwork" "public_subnet_vm_2" {
  name          = "public-subnet-vm-2"
  ip_cidr_range = "10.192.11.0/24"
  region        = var.region
  network       = google_compute_network.vpc_network.id
}

