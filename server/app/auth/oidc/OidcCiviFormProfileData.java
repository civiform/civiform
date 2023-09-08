package auth.oidc;

import auth.AccountCreator;
import auth.CiviFormProfileData;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.OidcProfileDefinition;
import repository.DatabaseExecutionContext;

/**
 * OIDC-specific implementation of the CiviFormProfileData interface. For deployments that use OIDC,
 * instances of this class will be saved in the session store.
 */
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
    // (e.g., Okta) for proper logout behavior. We override the implementation to retain the
    // ID_TOKEN.
    removeAttribute(OidcProfileDefinition.ACCESS_TOKEN);
    removeAttribute(OidcProfileDefinition.REFRESH_TOKEN);
  }
}
