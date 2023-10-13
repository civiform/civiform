package auth.oidc.applicant;

import auth.ProfileFactory;
import auth.oidc.IdTokensFactory;
import com.google.common.collect.ImmutableList;
import javax.inject.Provider;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import repository.AccountRepository;

/**
 * This class takes an existing CiviForm profile and augments it with the information from an AD
 * profile. Right now this is only extracting the email address, since that is all that AD provides
 * right now.
 */
public class GenericApplicantProfileCreator extends ApplicantProfileCreator {
  public GenericApplicantProfileCreator(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      IdTokensFactory idTokensFactory,
      Provider<AccountRepository> accountRepositoryProvider,
      String emailAttributeName,
      String localeAttributeName,
      ImmutableList<String> nameAttributeNames) {
    super(
        configuration,
        client,
        profileFactory,
        idTokensFactory,
        accountRepositoryProvider,
        emailAttributeName,
        localeAttributeName,
        nameAttributeNames);
  }
}
