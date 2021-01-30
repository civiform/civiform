package controllers;

import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Person;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.PersonRepository;

public class PostgresController extends Controller {

  private final FormFactory formFactory;
  private final PersonRepository personRepository;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public PostgresController(
      FormFactory formFactory,
      PersonRepository personRepository,
      HttpExecutionContext httpExecutionContext) {
    this.formFactory = formFactory;
    this.personRepository = personRepository;
    this.httpExecutionContext = httpExecutionContext;
  }

  public CompletionStage<Result> index() {
    // Run a db operation in another thread (using DatabaseExecutionContext)
    return personRepository
        .list()
        .thenApplyAsync(
            list -> {
              String out = "";
              for (Person p : list) {
                out = out.concat(p.id + ": " + p.name + "\n");
              }
              return ok(out);
            },
            httpExecutionContext.current());
  }

  public CompletionStage<Result> create(Http.Request request) {
    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    String name = requestData.get("name");
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

  public Result createSync(Http.Request request) {
    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    String name = requestData.get("name");
    Person p = new Person();
    p.name = name;
    p.save();
    return ok("person " + name + " with ID: " + p.id.toString() + " synchronously created.");
  }
}
