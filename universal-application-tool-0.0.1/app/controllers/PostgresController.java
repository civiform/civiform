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

  public CompletionStage<Result> list() {
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

  public CompletionStage<Result> add(String name) {
    Person p = new Person();
    p.name = name;
    return personRepository
        .insert(p)
        .thenApplyAsync(
            id -> {
              return ok("person " + name + " with ID: " + id.toString() + " added.");
            },
            httpExecutionContext.current());
  }

  public Result syncAdd(String name) {
    Person p = new Person();
    p.id = System.currentTimeMillis(); // not ideal, but it works
    p.name = name;
    p.save();
    return ok("person " + name + " with ID: " + p.id.toString() + " synchronously added.");
  }
}
