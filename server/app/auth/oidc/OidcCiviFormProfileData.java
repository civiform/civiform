package auth.oidc;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfileData;
import com.google.common.base.Preconditions;
import models.Account;
import models.Applicant;
import org.pac4j.oidc.profile.OidcProfile;
import repository.DatabaseExecutionContext;

public class OidcCiviFormProfileData extends OidcProfile implements CiviFormProfileData {
  public OidcCiviFormProfileData() {
    super();
  }

  public OidcCiviFormProfileData(Long accountId) {
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
