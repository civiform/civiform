# universal-application-tool


To build the container that runs the app, type `docker build -t uat .`

To run the application, run `docker run -it -p 9000:9000 -v ${PWD}/universal-application-tool-0.0.1:/code uat sbt ~run`
This enables hot-reloading - when you modify files, the server will recompile and restart.
After this, you can access the server at localhost:9000.

To launch postgres and adminer along with play app, run `./bin/run-dev`
This will start 3 individual containers.
You can access play app at localhost:9000 as before, and adminer at localhost:8080.
