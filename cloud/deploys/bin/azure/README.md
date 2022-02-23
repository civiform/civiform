# azure/bin
This directory includes scripts to help with managing staging and production environments in Azure.


## Bastion instance

### db-connection
Establishes a db-connection via the bastion. 
- Adds the IP address to the security rule & allows
ssh connections to the bastion
- Creates a new ssh key and associates it with the bastion instance.
- Opens a postgres connection to the remote database and forwards the terminal along.
- On closing the connection deletes the ssh key & updates the ssh security rule to deny all requests

After running this you should be able to run postgres commands directly. 

### Known issues
To run a pg_dump/restore on a newly provisioned machine will need to 
update the postgres version (instructions at the stack overflow below)
if it's a newly provisioned machine follow the stackoverlow linked [here](https://stackoverflow.com/questions/52765873/how-to-upgrade-to-postgresql-11-for-ubuntu-18-04)


## Key Vault

## Key Vault Setup
Creates a key vault instance and stores generated values for the postgres admin password and application secret key. Before running this script, make sure that you have the "Key Vault Secrets Officer" permission assigned to your Azure user account. For instructions on how to assign Azure roles, see [Assigning Azure Roles using Azure CLI](https://docs.microsoft.com/en-us/azure/role-based-access-control/role-assignments-cli).
