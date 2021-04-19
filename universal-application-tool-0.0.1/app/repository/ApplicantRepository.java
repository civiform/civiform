package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
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
  private final Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public ApplicantRepository(
      EbeanConfig ebeanConfig,
      DatabaseExecutionContext executionContext,
      Provider<VersionRepository> versionRepositoryProvider) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
  }

  public CompletionStage<Set<Applicant>> listApplicants() {
    return supplyAsync(() -> ebeanServer.find(Applicant.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(long id) {
    return supplyAsync(
        () -> ebeanServer.find(Applicant.class).setId(id).findOneOrEmpty(), executionContext);
  }

  /**
   * Returns all programs that are appropriate to serve to an applicant - which is any active
   * program, plus any program where they have an application in the draft stage.
   */
  public CompletionStage<ImmutableList<ProgramDefinition>> programsForApplicant(long applicantId) {
    return supplyAsync(
            () -> {
              List<Program> inProgressPrograms =
                  ebeanServer
                      .find(Program.class)
                      .alias("p")
                      .where()
                      .exists(
                          ebeanServer
                              .find(Application.class)
                              .where()
                              .eq("applicant.id", applicantId)
                              .eq("lifecycle_stage", LifecycleStage.DRAFT)
                              .raw("program.id = p.id")
                              .query())
                      .endOr()
                      .findList();
              List<Program> activePrograms =
                  versionRepositoryProvider.get().getActiveVersion().getPrograms();
              return new ImmutableList.Builder<Program>()
                  .addAll(activePrograms)
                  .addAll(inProgressPrograms)
                  .build();
            },
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
                      .max(Comparator.comparing(compared -> compared.getWhenCreated())));
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
    if (left.getWhenCreated().isAfter(right.getWhenCreated())) {
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
