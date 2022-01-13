# Azure Terraform Setup

## create a backend_vars file 

Create a file named `backend_vars` and add the following values to it:
```
resource_group_name  = < rg group name >
storage_account_name  = < account name >
container_name  = < container name >
key  = "terraform.tfstate"
```
You can get the resource group name, the storage account name, and the container name from the azure portal (or copy the file from a collaborator or shared private repo). 

## export your account key
In order to get the `account_key` value, use the azure CLI. You'll need to get the `resource_group_name` and `storage_account_name` from the prior step.
```
ACCOUNT_KEY=$(az storage account keys list --resource-group $RESOURCE_GROUP_NAME --account-name $STORAGE_ACCOUNT_NAME --query '[0].value' -o tsv)
export ARM_ACCESS_KEY=$ACCOUNT_KEY
```

## running terraform with this config  
Since we are using a shared backend you'll need to run init specifying that
```
$ terraform init -backend-config=backend_vars
```

# First time setup
In order to run for the first time we have to create the blob store to store the backend data in. 
```
$ chmod +x initial_setup.sh
$ source initial_setup.sh
```

This will set up a resource group for terraform shared state to live in. It will also make a blob storage account for the shared tfstate to live in. Finally it will create a backend_vars file that will be used when running terraform to share the state. Note that it takes azure a few minutes for you to be able to access the blob storage and that you'll need to share the backend_vars with other people developing on your project. You can find the info on the azure portal as well. 
