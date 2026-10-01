package auth.oidc.applicant;

import auth.oidc.OidcClientProviderParams;
import auth.oidc.StandardClaimsAttributeNames;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

/**
 * This class takes an existing CiviForm profile and augments it with the information from an AD
 * profile. Right now this is only extracting the email address, since that is all that AD provides
 * right now.
 */
public class GenericApplicantProfileCreator extends ApplicantProfileCreator {
  public GenericApplicantProfileCreator(
      OidcConfiguration configuration,
      OidcClient client,
      OidcClientProviderParams params,
      StandardClaimsAttributeNames standardClaimsAttributeNames) {
    super(configuration, client, params, standardClaimsAttributeNames);
  }
}
