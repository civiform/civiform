package repository;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Program;
import models.Question;
import play.db.ebean.EbeanConfig;

public class ApplicantRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ApplicantRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
    this.executionContext = executionContext;
  }

  public CompletionStage<Set<Applicant>> listApplicants() {
    return supplyAsync(() -> ebeanServer.find(Applicant.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(Applicant.class).setId(id).findOne()),
        executionContext);
  }

  public CompletionStage<Void> insertApplicant(Applicant applicant) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(applicant);
          return null;
        },
        executionContext);
  }

}
