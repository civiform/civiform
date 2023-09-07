package auth.oidc;

import auth.AccountCreator;
import auth.CiviFormProfileData;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.OidcProfileDefinition;
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

  @Override
  public void removeLoginData() {
    // The base implementation removes the ID_TOKEN, which is required by some identity providers
    // (e.g., Okta) for proper logout behavior.
    removeAttribute(OidcProfileDefinition.ACCESS_TOKEN);
    removeAttribute(OidcProfileDefinition.REFRESH_TOKEN);
  }
}
