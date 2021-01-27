package controllers;

import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Person;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import repository.PersonRepository;

public class PostgresController extends Controller {

  private final PersonRepository personRepository;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public PostgresController(
      PersonRepository personRepository, HttpExecutionContext httpExecutionContext) {
    this.personRepository = personRepository;
    this.httpExecutionContext = httpExecutionContext;
  }

  public CompletionStage<Result> index() {
    // Run a db operation in another thread (using DatabaseExecutionContext)
    return personRepository
        .list()
        .thenApplyAsync(
            list -> {
              String out = new String();
              for (Person p : list) {
                out = out.concat(p.id + ": " + p.name + "\n");
              }
              return ok(out);
            },
            httpExecutionContext.current());
  }

  public CompletionStage<Result> create(String name) {
    Person p = new Person();
    p.name = name;
    return personRepository
        .insert(p)
        .thenApplyAsync(
            id -> {
              return ok("person " + name + " with ID: " + id.toString() + " created.");
            },
            httpExecutionContext.current());
  }

  public Result createSync(String name) {
    Person p = new Person();
    p.name = name;
    p.save();
    return ok("person " + name + " with ID: " + p.id.toString() + " synchronously created.");
  }
}
