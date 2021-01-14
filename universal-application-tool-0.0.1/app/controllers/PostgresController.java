package controllers;

import models.PostgresDatabase;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class PostgresController extends Controller {
  public static String homePageString = "This page has been retrieved ";

  private final PostgresDatabase db;
  private final HttpExecutionContext ec;

  @Inject
  public PostgresController(PostgresDatabase db, HttpExecutionContext ec) {
    this.db = db;
    this.ec = ec;
  }

  public CompletionStage<Result> retrieve() {
    return db.updateSomething()
        .thenApplyAsync(
            result -> ok(homePageString + String.valueOf(result) + " times."), ec.current());
  }
}
