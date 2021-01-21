# universal-application-tool

## Running a local server

To build the container that runs the app, type `docker build -t uat .`

To run the application, run `docker run -it -p 9000:9000 -v ${PWD}/universal-application-tool-0.0.1:/code uat sbt ~run`
This enables hot-reloading - when you modify files, the server will recompile and restart.
After this, you can access the server at localhost:9000.

To launch postgres and adminer along with play app, run `./bin/run-dev`
This will start 3 individual containers.
You can access play app at localhost:9000 as before, and adminer at localhost:8080.

## Development standards

### Client-server concerns

The client should be as simple as is practical to implement the desired user experience.

### Java code

Java code should conform to the Google Java [styleguide](https://google.github.io/styleguide/javaguide.html). The project makes use of a linter and autoformatter for Java to help with this.

We have an autoformatter if there isn't one in your IDE - just run `bin/fmt` and your code should be automatically formatted.

Prefer using immutable collection types provided by [Guava](https://github.com/google/guava) ([API docs](https://guava.dev/releases/snapshot/api/docs/)) over the Java standard library's mutable collections unless impractical. Include a comment justifying the use of a mutable collection if you use one.

#### Separation of concerns

See [wikipedia definition](https://en.wikipedia.org/wiki/Separation_of_concerns).

In lieu of a microservice architecture, this project necessitates special care in ensuring separation of concerns. While Play provides some structure in this regard, it should be viewed as a starting point.

Code in **Play controllers** should be limited to brokering interaction between the server's business logic systems and HTTP. Code in controllers should never directly implement business logic concerns and should instead delegate to classes specific to those purposes. One way to help think about this when writing controller code: if you're writing an HTTP handler that responds with HTML, factor out business logic classes so that implementing another handler that performs the same logic but responds with JSON benefits from high code re-use.

Code in **ebean models** should be limited to brokering interaction between the server's business logic and the database. Code in models should never directly implement business logic concerns.

### Testing

We aim for complete unit test coverage of all execution paths in the system. If you submit code that is infeasible or impractical to get full test coverage for, consider refactoring. If you would like to make an exception, include a clear explanation for why in your PR description.

All major user-facing features should be covered by a functional browser test.
