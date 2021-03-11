package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import java.time.Clock;
import java.time.ZoneId;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.CommonProfile;
import repository.DatabaseExecutionContext;

/**
 * This class is specifically intended to be serialized, encrypted, and stored in the Play session
 * cookie. It cannot contain anything that's not serializable - this includes database connections,
 * thread pools, etc.
 *
 * <p>It is wrapped by UatProfile, which is what we should use server-side.
 */
public class UatProfileData extends CommonProfile {
  private Clock clock;

  public UatProfileData() {
    this(Clock.system(ZoneId.of("America/Los_Angeles")));
  }

  public UatProfileData(Clock clock) {
    super();
    this.clock = Preconditions.checkNotNull(clock);
  }

  public UatProfileData(Clock clock, Long accountId) {
    this(clock);
    this.setId(accountId.toString());
  }

  /**
   * This method needs to be called outside the constructor since constructors should not do
   * database accesses (or other work). It should be called before the object is used - the object
   * has not been persisted / correctly created until it is called.
   */
  public void init(DatabaseExecutionContext dbContext) {
    if (this.getId() != null && !this.getId().isEmpty()) {
      return;
    }
    // We use this async only to make sure we run in the db execution context - this method cannot
    // be
    // asynchronous because the security code that executes it is entirely synchronous.
    supplyAsync(
            () -> {
              Account acc = new Account();
              acc.save();
              Applicant newA = new Applicant();
              newA.getApplicantData().setCreatedTime(clock.instant());
              newA.setAccount(acc);
              newA.save();

              setId(Preconditions.checkNotNull(acc.id).toString());
              return null;
            },
            dbContext)
        .join();
  }
}
