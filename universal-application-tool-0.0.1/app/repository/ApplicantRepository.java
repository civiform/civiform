package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import play.db.ebean.EbeanConfig;
import services.program.ProgramDefinition;

public class ApplicantRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ApplicantRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
  }

  public CompletionStage<Set<Applicant>> listApplicants() {
    return supplyAsync(() -> ebeanServer.find(Applicant.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(long id) {
    return supplyAsync(
        () -> ebeanServer.find(Applicant.class).setId(id).findOneOrEmpty(), executionContext);
  }

  public CompletionStage<ImmutableList<ProgramDefinition>> programsForApplicant(long id) {
    return supplyAsync(
            () ->
                ebeanServer
                    .find(Program.class)
                    .alias("p")
                    .where()
                    .or()
                    .eq("lifecycle_stage", LifecycleStage.ACTIVE)
                    .exists(
                        ebeanServer
                            .find(Application.class)
                            .where()
                            .eq("applicant.id", id)
                            .raw("program.id = p.id")
                            .query())
                    .endOr()
                    .findList(),
            executionContext.current())
        .thenApplyAsync(
            (programs) ->
                programs.stream()
                    .map(program -> program.getProgramDefinition())
                    .collect(ImmutableList.toImmutableList()));
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(String emailAddress) {
    return supplyAsync(
        () -> {
          if (emailAddress == null || emailAddress.isEmpty()) {
            return Optional.empty();
          }
          Optional<Account> accountMaybe =
              ebeanServer
                  .find(Account.class)
                  .where()
                  .eq("email_address", emailAddress)
                  .findOneOrEmpty();
          // Return the applicant which was most recently created.
          return accountMaybe.flatMap(
              applicant ->
                  applicant.getApplicants().stream()
                      .max(
                          Comparator.comparing(
                              compared -> compared.getApplicantData().getCreatedTime())));
        },
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

  public CompletionStage<Void> updateApplicant(Applicant applicant) {
    return supplyAsync(
        () -> {
          ebeanServer.update(applicant);
          return null;
        },
        executionContext);
  }

  public Optional<Applicant> lookupApplicantSync(long id) {
    return ebeanServer.find(Applicant.class).setId(id).findOneOrEmpty();
  }

  /**
   * Merge the older applicant into the newer applicant, update the Account of the newer to point to
   * both the older and newer, and
   */
  public Applicant mergeApplicants(Applicant left, Applicant right) {
    if (left.getApplicantData()
        .getCreatedTime()
        .isAfter(right.getApplicantData().getCreatedTime())) {
      Applicant tmp = left;
      left = right;
      right = tmp;
    }
    // At this point, "left" is older, "right" is newer, we will merge "left" into "right", because
    // the newer applicant is always preferred when more than one applicant matches an account.
    left.setAccount(right.getAccount());
    right.getApplicantData().mergeFrom(left.getApplicantData());
    return right;
  }
}
