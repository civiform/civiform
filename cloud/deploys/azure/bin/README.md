# azure/bin
This directory includes two scripts to help set up the bastion instance. 

# db-connection
Establishes a db-connection via the bastion. Creates
a new ssh key and associates it with the bastion instance. Afterwards it opens a 
postgres connection to the remote database and forwards the terminal along.

After running this you should be able to run postgres commands directly. 

## Known issues
The first time you run this command on a newly provisioned machine, the 
command might fail. You can just re-run the command and it should run. You 
will also need to manually enter the password for the database

# get-bastion-commands
In order to run stuff like pg_dump/restore you have to ssh onto the machine
and not just run via the database. In order to do this get-bastion-commands
will grab the correct hosts and echo out the ssh commands

To run a pg_dump/restore on a newly provisioned machine will need to 
update the postgres version (instructions at the stack overflow below)
if it's a newly provisioned machine follow the stackoverlow linked [here](https://stackoverflow.com/questions/52765873/how-to-upgrade-to-postgresql-11-for-ubuntu-18-04)

# known issues
You will need to enter in the db password whenever you do database commands. I 
tried to pass along the password to the commands. 

