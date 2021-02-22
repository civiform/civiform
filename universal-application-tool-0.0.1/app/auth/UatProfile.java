package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import java.time.Clock;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.CommonProfile;

public class UatProfile extends CommonProfile {
  private final Clock clock;

  public UatProfile() {
    this(Clock.systemDefaultZone());
  }

  public UatProfile(Clock clock) {
    super();
    this.clock = clock;
  }

  public CompletableFuture<Applicant> getApplicant() {
    return this.getAccount()
        .thenApply(
            (a) ->
                a.getApplicants().stream()
                    .sorted(
                        Comparator.comparing(
                            (applicant) -> applicant.getApplicantData().getCreatedTime()))
                    .findFirst()
                    .orElseThrow());
  }

  public CompletableFuture<Account> getAccount() {
    return supplyAsync(
        () -> {
          Account account = new Account();
          account.id = Long.valueOf(this.getId());
          account.refresh();
          return account;
        });
  }

  /**
   * This method needs to be called outside the constructor since constructors should not do
   * database accesses (or other work). It should be called before the object is used - the object
   * has not been persisted / correctly created until it is called.
   */
  public void init() {
    Account acc = new Account();
    acc.save();
    Applicant newA = new Applicant();
    newA.getApplicantData().setCreatedTime(clock.instant());
    newA.setAccount(acc);
    newA.save();

    setId(Preconditions.checkNotNull(acc.id).toString());
  }
}
