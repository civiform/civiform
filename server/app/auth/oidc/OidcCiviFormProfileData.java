package auth.oidc;

import auth.AccountCreator;
import auth.CiviFormProfileData;
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
    var accountCreator = new AccountCreator(this);
    accountCreator.create(dbContext, this.getId());
  }
}
