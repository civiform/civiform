package repository;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import models.Person;
import play.db.ebean.EbeanConfig;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/** */
public class PersonRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public PersonRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
    this.executionContext = executionContext;
  }

  /** Return all persons in a set. */
  public CompletionStage<Set<Person>> list() {
    return supplyAsync(() -> ebeanServer.find(Person.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Person>> lookup(Long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(Person.class).setId(id).findOne()),
        executionContext);
  }

  public CompletionStage<Long> insert(Person person) {
    return supplyAsync(
        () -> {
          person.id = System.currentTimeMillis(); // not ideal, but it works
          ebeanServer.insert(person);
          return person.id;
        },
        executionContext);
  }
}
