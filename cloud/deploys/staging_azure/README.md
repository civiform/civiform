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

## Logging 
In order to see the log stream for your app service; you have to manually allow the http logs. Do this by going to diagnostic settings and send the http logs to the log server we created (note I think this can be done via terraform).

## Azure Ad Setup
Add the adfs_client_id to your local configs. For the private adfs_secret add it via key vault.

Configure the Microsoft provider:
- Go To App Serice and select the authentication tab on the left panel
- Add a new identity provider and select Microsoft
- Make sure the identity provider allows all requests (this page is publicly visible but the login is restricted)
- Make a note of the client id (as this will be the adfs_client_id to store in your client id)

Within the identity provider you just created go to Authentication and add the following information:
- Add the correct redirect url (should be: `https://<HOSTNAME>/callback/AdClient`) 
- Select ID token 
- Select Single Tenant 

Within the API permissions
- Select add permission
- Select delegated permissions
- Select microsoft graph
- Find the openid Permissions and select: email, openid, profile

Within the certificates & secrets
- add a new client secret and add the value to your key vault

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

# Configure Key Vault before running Terraform
Before applying the Terraform configuration, you'll need to make sure that Azure Key Vault is
properly configured to store the secrets needed by the application. To do this, run the command `key-vault-setup` in bin/azure. 

You will need to manually add the key vault values for:
- Azure AD
- AWS Secret

Via azure portal or the CLI command:
```
az keyvault secret set --name [KEY_NAME] --vault-name [key vault name] --value [KEY_VALUE]
```

## Manually configure the keyvault
If you want to do this all manually. List all the key vaults in your project. Find the key vault that stores the secrets for Civiform. Then run `az keyvault secret list --vault-name=[your vault name]`. The `postgres-password` and `app-secret-key` secrets should be listed.

If the secrets are not listed, you'll need to [create a key vault](https://docs.microsoft.com/en-us/azure/key-vault/general/quick-create-cli) if you haven't already. Make a note of the resource group that is used. Next, run 
```
az keyvault update --name=[your keyvault name] --enable-rbac-authorization
```
This allows you to grant the App Service Managed Identity permission to access the key vault.

Next, [grant yourself the Key Vault Secrets Officer role in the Azure portal](https://docs.microsoft.com/en-us/azure/key-vault/general/rbac-guide?tabs=azure-cli#key-vault-scope-role-assignment)
Then set the secrets by running:

```
az keyvault secret set --name [KEY_NAME] --vault-name [key vault name] --value [KEY_VALUE]
```

Then, in order to use the Key Vault as a data source in Terraform, set the `key_vault_name` variable in your `auto.tfvars` file to the name of the key vault and the `key_vault_resource_group` to the resource group the key vault is in.

# Configuring the staging domain
The terraform script configures the azure app service to allow requests from the staging hostname that you pass in via the environment variables, but you will need to manually add a cname and txt configuration to your domain provider (e.g https://domains.google.com). 

To do that add the custom records via the domain provider webiste. 
1) CNAME record which points from the 'staging-azure.civiform.dev' to the hostname that gets generated from terraform (you can find this from the terraform output or via the azure portal)
2) TXT record with key 'asuid.staging-azure.civiform.dev' and value that matches the custom domain verification id in the azure portal (you can find this by navigating to the custom domains in the app service setting). 

Note it should take a few minutes to propagate.