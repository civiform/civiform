package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.CommonProfile;
import repository.DatabaseExecutionContext;

/**
 * A fake implementation of the CiviFormProfileData interface, not tied to any authentication
 * mechanism.
 *
 * <p>Should only be used in tests.
 */
public class FakeCiviFormProfileData extends CommonProfile implements CiviFormProfileData {
  public FakeCiviFormProfileData() {
    super();
  }

  public FakeCiviFormProfileData(Long accountId) {
    this();
    this.setId(accountId.toString());
  }

  @Override
  public void init(DatabaseExecutionContext dbContext) {
    if (this.getId() != null && !this.getId().isEmpty()) {
      return;
    }
    // We use this async only to make sure we run in the db execution context - this method cannot
    // be asynchronous because the security code that executes it is entirely synchronous.
    supplyAsync(
            () -> {
              Account acc = new Account();
              acc.save();
              Applicant newA = new Applicant();
              newA.setAccount(acc);
              newA.save();

              setId(Preconditions.checkNotNull(acc.id).toString());
              return null;
            },
            dbContext)
        .join();
  }
}
