package auth;

import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.credentials.AnonymousCredentials;
import org.pac4j.core.util.HttpActionHelper;

public class GuestClient extends IndirectClient {

  private ProfileFactory profileFactory;

  @Inject
  public GuestClient(ProfileFactory profileFactory) {
    this.profileFactory = profileFactory;
  }

  @Override
  protected void internalInit() {
    // There will never be any credentials, so we just use this sentinel value.
    // This will be passed as the first argument to the authenticator we define below.
    defaultCredentialsExtractor((ctx, store) -> Optional.of(new AnonymousCredentials()));
    defaultAuthenticator(
        (cred, ctx, store) -> cred.setUserProfile(profileFactory.createNewApplicant()));
    defaultRedirectionActionBuilder(
        (ctx, store) -> Optional.of(HttpActionHelper.buildRedirectUrlAction(ctx, "/")));
  }
}
