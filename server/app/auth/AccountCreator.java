package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.UserProfile;
import repository.DatabaseExecutionContext;

/** Class to handle account creation, if required, for a `UserProfile`. */
public final class AccountCreator {
  private UserProfile profile;

  public AccountCreator(UserProfile profile) {
    this.profile = profile;
  }

  /** Creates an account in the db for `accountId` if one does not already exist. */
  public void create(DatabaseExecutionContext dbContext, String accountId) {
    if (accountId != null && !accountId.isEmpty()) {
      return;
    }
    // We use this async supply function to ensure this runs in the db execution context. This
    // method cannot execute asynchronously because the security code that executes it is entirely
    // synchronous.
    supplyAsync(
            () -> {
              Account acc = new Account();
              acc.save();
              Applicant newA = new Applicant();
              newA.setAccount(acc);
              newA.save();

              profile.setId(Preconditions.checkNotNull(acc.id).toString());
              return null;
            },
            dbContext)
        .join();
  }
}
