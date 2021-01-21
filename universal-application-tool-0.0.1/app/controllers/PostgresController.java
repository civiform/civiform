package controllers;

import models.Person;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import repository.PersonRepository;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

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
}
