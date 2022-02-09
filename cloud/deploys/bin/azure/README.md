# azure/bin
This directory includes two scripts to help set up the bastion instance. 

# db-connection
Establishes a db-connection via the bastion. Creates
a new ssh key and associates it with the bastion instance. Afterwards it opens a 
postgres connection to the remote database and forwards the terminal along.

After running this you should be able to run postgres commands directly. 

# Known issues
You will need to enter in the db password whenever you do database commands. I 
tried to pass along the password to the commands, but getting this working is 
tbd

To run a pg_dump/restore on a newly provisioned machine will need to 
update the postgres version (instructions at the stack overflow below)
if it's a newly provisioned machine follow the stackoverlow linked [here](https://stackoverflow.com/questions/52765873/how-to-upgrade-to-postgresql-11-for-ubuntu-18-04)
