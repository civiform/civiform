## Azure/bin scripts

### db-connection
Establishes a db-connection via the bastion. 
- Adds the IP address to the security rule & allows
ssh connections to the bastion
- Creates a new ssh key and associates it with the bastion instance.
- Opens a postgres connection to the remote database and forwards the terminal along.
- On closing the connection deletes the ssh key & updates the ssh security rule to deny all requests

After running this you should be able to run postgres commands directly. 

### pg-restore
Does a data only pg_restore onto a remote database. Does the similar setting up the ip address & security rules as the db connection but instead of connecting to the database does a pg_restore instead.

### Known issues
To run a pg_dump/restore on a newly provisioned machine will need to 
update the postgres version (instructions at the stack overflow below)
if it's a newly provisioned machine. There are possible some version issues with this script (should read something like incompotible postgres versions). We try to correct for that in the script by downloading and using a pinned version of pg_restore. The non direct absolute path pg_restore uses a sym linked pg_wrapper class that points to an old version of postgres. 

You might also run into an issue with pg_restore if you do not include the .dump at the end of your file name for the dump file. Postgres expects this and will not recognize it as a dump file if you don't include it!=