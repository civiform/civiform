package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Account;
import play.db.ebean.EbeanConfig;

public class AccountRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public AccountRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
  }

  public CompletionStage<Void> insertAccount(Account account) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(account);
          return null;
        },
        executionContext);
  }
}
