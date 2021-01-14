package models;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.db.Database;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class PostgresDatabase {

  private Database db;
  private DatabaseExecutionContext executionContext;

  @Inject
  public PostgresDatabase(Database db, DatabaseExecutionContext executionContext) {
    this.db = db;
    this.executionContext = executionContext;
  }

  public CompletionStage<Integer> updateSomething() {
    return CompletableFuture.supplyAsync(
        () -> {
          return db.withConnection(
              connection -> {
                // do whatever you need with the db connection
                return 10;
              });
        },
        this.executionContext);
  }
}
