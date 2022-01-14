# Azure Terraform Setup
We use a remote backend to store state information to avoid conflicts when 
multiple collaborators are updating resources managed by Terraform. 
Follow the steps outlined in this doc to enable collaborators to access 
the remote backend and shared state on their machines.

# First time setup
In order to run for the first time per project we have to create the blob store
 to store the backend data in. This script is really only set up to be run one 
 time and if you run it again it will rerun steps 2 and 3. 

WARNING: running this script repeatedly will set up repeated blob storage 
accounts! Go delete them if you run it more than once. B/c storage accounts 
have to be globally unique we have a random number generator here but it 
doesn't do any fancy state tracking so everytime you run this it's another
random number
```
$ source initial_setup
```

This will: 
1. set up a resource group for terraform shared state to live in
2. make a blob storage account for the shared tfstate to live in
3. create a backend_vars file that will be used when running terraform to 
share the state. 

Note that it takes azure a few minutes for you to be able to access the blob 
storage and that you'll need to share the backend_vars with other people 
developing on your project. You can find the info on the azure portal as well. 

# Non Initial Setup
The `initial_setup` script will create and set up the environment variables 
that are necessary to run terraform commands with shared state. If there is 
already shared state you can just create a backend_vars file, 
export the account key, run the correct init (detailed below) and then 
run commands as normal 

## Create a backend_vars file 

Create a file named `backend_vars` within the folder you run terraform in 
and add the following values to it:
```
resource_group_name  = tfstate
storage_account_name  = < account name >
container_name  = tfstate
key  = "terraform.tfstate"
```

### How to get the storage_account_name
You can get the the storage account name from the azure portal or copy the 
file from a collaborator or store it in a shared private repo. That variable 
isn't private just specific to each deployment. 

To find this in the azure portal look for the resource group and find the 
storage account within that resource group. For example in the below azure 
portal the resource group name is 'tfstate' and the storage_account_name is 
tfstate7307.
![Image of Azure portal showing where to find the storage_account_name](img/how_to_find_backend_vars.png?raw=true)

## Export your account key
In order to get the `account_key` value, use the azure CLI. You'll need to get 
the `resource_group_name` and `storage_account_name` from the prior step.
```
ACCOUNT_KEY=$(az storage account keys list --resource-group \
$RESOURCE_GROUP_NAME --account-name $STORAGE_ACCOUNT_NAME --query \
'[0].value' -o tsv)
export ARM_ACCESS_KEY=$ACCOUNT_KEY
```

## Running terraform with this config  
Since we are using a shared backend you'll need to run init specifying that
```
$ terraform init -backend-config=backend_vars
```

# Troubleshooting

## Access Issues
If you are running into access issues, you do need to have the ARM_ACCESS_KEY 
exported to run terraform commands. So try re running
```
ACCOUNT_KEY=$(az storage account keys list --resource-group \
$RESOURCE_GROUP_NAME --account-name $STORAGE_ACCOUNT_NAME --query \
'[0].value' -o tsv)
export ARM_ACCESS_KEY=$ACCOUNT_KEY
```

## backend_vars issues
If you run the setup script more than once your backend_vars will be incorrect 
you only need the setup for the storage container you are planing on using and 
you can delete the other ones