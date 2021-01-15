# universal-application-tool


To build the container that runs the app, type `docker build -t uat .`

To run the application, run `docker run -it -p 9000:9000 -v ${PWD}/universal-application-tool-0.0.1:/code uat sbt ~run`
This enables hot-reloading - when you modify files, the server will recompile and restart.
After this, you can access the server at localhost:9000.

To launch postgres and adminer along with play app, run `./bin/run-dev`
This will start 3 individual containers.
You can access play app at localhost:9000 as before, and adminer at localhost:8080.

To format java code locally, run './bin/fmt', from either this directory or the root of the project.

## Database Schema Management
In order to enable the frequent changes to the schema that we expect in the early days of this project, we are using the `migrate` tool.
Fundamentally this is a tool which treats a database schema as a sequence of statements to apply.  In order to ensure that everyone is able to
migrate from early versions to later versions, as well as vice-versa, these statements are generated in pairs - `up` and `down` - and ordered.

You can read a lot about this tool [here](https://github.com/golang-migrate/migrate/tree/master/cmd/migrate).
It is provided in a convenient docker container, and configuration for connecting that container to the postgres container we have in
our docker-compose stack is provided in `./bin/schematool`.  Some handy commands are:
  - `./bin/schematool` - bring database to most recent version.  (apply all migrations in order)
  - `./bin/schematool create -seq -ext sql -dir /migrations some_name` - create an empty new set of migrations (up and down) with name `some_name`.
  - `./bin/schematool down -all` - reset database to empty.
  - `./bin/schematool up 5` - bring database to migration number "5", if currently version is lower than 5.
  - `./bin/schematool down 5` - bring database to migration number "5", if currently version is higher than 5.

We also have a command to completely destroy and recreate the entire database - `./bin/clear-dev-db`.  This command only works if the database is currently stopped, and requires root permissions since the db uses a non-default user to store its data.
