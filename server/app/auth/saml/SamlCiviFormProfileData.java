package auth.saml;

import auth.AccountCreator;
import auth.CiviFormProfileData;
import org.pac4j.saml.profile.SAML2Profile;
import repository.DatabaseExecutionContext;

/**
 * SAML-specific implementation of the CiviFormProfileData interface. For deployments that use SAML,
 * instances of this class will be saved in the session store.
 */
public class SamlCiviFormProfileData extends SAML2Profile implements CiviFormProfileData {
  public SamlCiviFormProfileData() {
    super();
  }

  public SamlCiviFormProfileData(Long accountId) {
    this();
    this.setId(accountId.toString());
  }

  @Override
  public void init(DatabaseExecutionContext dbContext) {
    var accountCreator = new AccountCreator(this);
    accountCreator.create(dbContext, this.getId());
  }
}
