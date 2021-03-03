package auth;

import com.google.common.base.Preconditions;
import controllers.admin.routes;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.credentials.AnonymousCredentials;
import org.pac4j.core.util.HttpActionHelper;

public class FakeAdminClient extends IndirectClient {

  public static final String CLIENT_NAME = "FakeAdminClient";

  private ProfileFactory profileFactory;

  @Inject
  public FakeAdminClient(ProfileFactory profileFactory) {
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
  }

  @Override
  protected void internalInit() {
    defaultCredentialsExtractor(
        (ctx, store) -> {
          // Double check that we haven't been fooled into allowing this somehow.
          if (!ctx.getServerName().equals("localhost")) {
            throw new UnsupportedOperationException(
                "You cannot create a fake admin unless you are running locally.");
          }
          return Optional.of(new AnonymousCredentials());
        });
    defaultAuthenticator(
        (cred, ctx, store) -> cred.setUserProfile(profileFactory.createNewAdmin()));
    defaultRedirectionActionBuilder(
        (ctx, store) ->
            Optional.of(
                HttpActionHelper.buildRedirectUrlAction(
                    ctx, routes.AdminProgramController.index().url())));
  }
}
