package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import play.db.ebean.EbeanConfig;

public class ApplicantRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;
  // This is needed in the auth.UatProfileAdapter family of classes, which need to be
  // initialized before the client objects.  At that time, the ebeanConfig is not available,
  // so it's impossible to inject this repository into that object.
  // TODO(someone with guice experience): is there a better way?
  public static ApplicantRepository INSTANCE;

  @Inject
  public ApplicantRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
    INSTANCE = this;
  }

  public CompletionStage<Set<Applicant>> listApplicants() {
    return supplyAsync(() -> ebeanServer.find(Applicant.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(long id) {
    return supplyAsync(
        () -> ebeanServer.find(Applicant.class).setId(id).findOneOrEmpty(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(String emailAddress) {
    return supplyAsync(
        () -> {
          Optional<Account> account =
              ebeanServer
                  .find(Account.class)
                  .where()
                  .eq("email_address", emailAddress)
                  .findOneOrEmpty();
          if (account.isEmpty()) {
            return Optional.empty();
          }
          // Return the applicant which was most recently created.
          return account.get().getApplicants().stream()
              .max(
                  Comparator.comparing(applicant -> applicant.getApplicantData().getCreatedTime()));
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
