# Azure Terraform Setup
We use a remote backend to store state information to avoid conflicts when 
multiple collaborators are updating resources managed by Terraform. 
Follow the steps outlined in this doc to enable collaborators to access 
the remote backend and shared state on their machines.

# Setup
In order to run for the first time run from the `cloud/deploys/staging_azure` 
directory.
 
```
$ source setup
```

This will: 
1. set up a resource group for terraform shared state to live in if it does
not already exist
2. make a blob storage account for the shared tfstate to live in if it does
not already exist
3. create a backend_vars file that will be used when running terraform to 
share the state (will overwrite the existing)

Note that it takes azure a few minutes for you to be able to access the blob 
storage and that you'll need to share the backend_vars with other people 
developing on your project. You can find the info on the azure portal as well. 

You can run this command again if you delete your backend_vars or need to
re-export your account key (although you can also re-export your account key
via the commands below), note that it does a pretty fuzzy regex match for
the storage account name so if you are doing something special you might have
to write your own script to do that!

## Export your account key
In order to use terraform with the azure backend you'll need to get the 
`account_key` set up properly. The backend_vars file expects it to be provided 
via the `ARM_ACCESS_KEY` environment variable so you'll need to use the Azure CLI to provide it. 
Get the `resource_group_name` and `storage_account_name` from the backend_vars
file or via the azure cli/portal (details on how to do this in the 
troubleshooting section below)
```
ACCOUNT_KEY=$(az storage account keys list \
  --resource-group $RESOURCE_GROUP_NAME \
  --account-name $STORAGE_ACCOUNT_NAME \
  --query '[0].value' \
  -o tsv)
export ARM_ACCESS_KEY=$ACCOUNT_KEY
```

## Running terraform with this config  
Since we are using a shared backend you'll need to run init specifying that 
within the `cloud/deploys/staging_azure` directory.
```
$ terraform init -backend-config=backend_vars
```

# Troubleshooting

## Access Issues
If you are running into access issues, you do need to have the ARM_ACCESS_KEY 
exported to run terraform commands. So try re running
```
ACCOUNT_KEY=$(az storage account keys list \
  --resource-group $RESOURCE_GROUP_NAME \
  --account-name $STORAGE_ACCOUNT_NAME \
  --query '[0].value' \
  -o tsv)
export ARM_ACCESS_KEY=$ACCOUNT_KEY
```

## backend_vars file structure

This is used by terraform to specify the backend we wil use to store tfsate 
and generally should look like this
```
resource_group_name  = tfstate
storage_account_name = < account name >
container_name       = tfstate
key                  = "terraform.tfstate"
```

## Where to get the resource_group_name and the storage_account_name

You can use the [azure cli](https://docs.microsoft.com/en-us/cli/azure/)
or go to azure portal to get the resource_group_name
and the storage_account_name if you need to.

To find this in the azure portal look for the resource group and find the 
storage account within that resource group. For example in the below azure 
portal the resource group name is 'tfstate' and the storage_account_name is 
'tfstate7307'.
![Image of Azure portal showing where to find the storage_account_name](img/how_to_find_backend_vars.png?raw=true)

# Manually Configure Key Vault before running Terraform
Before applying the Terraform configuration, you'll need to make sure that Azure Key Vault is
properly configured to store the `postgres-password` and `app-secret-key` secrets. To do this, run the command `az keyvault list` to list all the key vaults in your project. Find the key vault that stores the secrets for Civiform. Then run `az keyvault secret list --vault-name=[your vault name]`. The `postgres-password` and `app-secret-key` secrets should be listed.

If the secrets are not listed, you'll need to [create a key vault](https://docs.microsoft.com/en-us/azure/key-vault/general/quick-create-cli) if you haven't already. Make a note of the resource group that is used. Next, run 
```
az keyvault update --name=[your keyvault name] --enable-rbac-authorization
```
This allows you to grant the App Service Managed Identity permission to access the key vault.

Next, [grant yourself the Key Vault Secrets Officer role in the Azure portal](https://docs.microsoft.com/en-us/azure/key-vault/general/rbac-guide?tabs=azure-cli#key-vault-scope-role-assignment)
Then set the secrets:

```
az keyvault secret set --name postgres-password --vault-name [key vault name] --value [password]
az keyvault secret set --name app-secret-key --vault-name [key vault name] --value [secret key]
```

Then, in order to use the Key Vault as a data source in Terraform, set the `key_vault_name` variable in your `auto.tfvars` file to the name of the key vault and the `key_vault_resource_group` to the resource group the key vault is in.
