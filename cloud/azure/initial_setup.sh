#!/bin/bash

RESOURCE_GROUP_NAME=tfstate
STORAGE_ACCOUNT_NAME=tfstate$RANDOM
CONTAINER_NAME=tfstate

echo "Create resource group"
az group create --name $RESOURCE_GROUP_NAME --location eastus

echo "Create storage account"
az storage account create --resource-group $RESOURCE_GROUP_NAME --name $STORAGE_ACCOUNT_NAME --sku Standard_LRS --encryption-services blob

echo "Create blob container"
az storage container create --name $CONTAINER_NAME --account-name $STORAGE_ACCOUNT_NAME

echo "storing the account key"
ACCOUNT_KEY=$(az storage account keys list --resource-group $RESOURCE_GROUP_NAME --account-name $STORAGE_ACCOUNT_NAME --query '[0].value' -o tsv)
export ARM_ACCESS_KEY=$ACCOUNT_KEY

echo "genering the backend vars file"
touch backend_vars
echo "resource_group_name  = \"$RESOURCE_GROUP_NAME\"" >> backend_vars
echo "storage_account_name  = \"$STORAGE_ACCOUNT_NAME\"" >> backend_vars
echo "container_name  = \"$CONTAINER_NAME\"" >> backend_vars
echo "key  = \"terraform.tfstate\"" >> backend_vars


