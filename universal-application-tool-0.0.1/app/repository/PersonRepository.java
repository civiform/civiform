package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Person;
import play.db.ebean.EbeanConfig;

/** */
public class PersonRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public PersonRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
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
          ebeanServer.insert(person);
          return person.id;
        },
        executionContext);
  }
}
