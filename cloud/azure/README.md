# Azure Terraform 

## Overview
Currently the `main.tf` file includes the main terraform configuration file that sets up the different services that civiform uses in Azure. 

## Variables
`main.tf` uses variables via `var.var_name`. Those variable definitions and defaults live in `variables.tf`. For values that need to be private create a `.auto.tfvars` or `terraform.tfvars` file which should not be checked into a public repo (as it will often contain private information). We have added an example file to copy and fill out with specific to deployment infromation
