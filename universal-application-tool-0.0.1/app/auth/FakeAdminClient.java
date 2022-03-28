package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.admin.routes;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.credentials.AnonymousCredentials;
import org.pac4j.core.util.HttpActionHelper;

/**
 * This class implements a special client that allows logging in without logging in to a real AD
 * account. The feature is only enabled in development environment.
 */
public class FakeAdminClient extends IndirectClient {

  public static final String CLIENT_NAME = "FakeAdminClient";
  public static final String GLOBAL_ADMIN = "GLOBAL";
  public static final String PROGRAM_ADMIN = "PROGRAM";
  public static final String DUAL_ADMIN = "DUAL";

  private ImmutableSet<String> acceptedHosts;
  private ProfileFactory profileFactory;

  @Inject
  public FakeAdminClient(ProfileFactory profileFactory, Config configuration) {
    this.profileFactory = checkNotNull(profileFactory);

    String stagingHostname = checkNotNull(configuration).getString("staging_hostname");
    acceptedHosts = ImmutableSet.of("localhost", "civiform", stagingHostname);
  }

  public boolean canEnable(String host) {
    return acceptedHosts.stream()
        .anyMatch(acceptedHost -> host.equals(acceptedHost) || host.startsWith(acceptedHost + ":"));
  }

  @Override
  protected void internalInit(final boolean forceReinit) {
    defaultCredentialsExtractor(
        (ctx, store) -> {
          // Double check that we haven't been fooled into allowing this somehow.
          if (!acceptedHosts.contains(ctx.getServerName())) {
            throw new UnsupportedOperationException(
                "You cannot create a fake admin unless you are running locally.");
          }
          return Optional.of(new AnonymousCredentials());
        });
    defaultAuthenticator(
        (cred, ctx, store) -> {
          Optional<String> adminType = ctx.getRequestParameter("adminType");
          if (adminType.isEmpty()) {
            throw new IllegalArgumentException("no admin type provided.");
          }
          if (adminType.get().equals(GLOBAL_ADMIN)) {
            cred.setUserProfile(profileFactory.createNewAdmin());
          } else if (adminType.get().equals(PROGRAM_ADMIN)) {
            cred.setUserProfile(profileFactory.createFakeProgramAdmin());
          } else if (adminType.get().equals(DUAL_ADMIN)) {
            cred.setUserProfile(profileFactory.createFakeDualAdmin());
          } else {
            throw new IllegalArgumentException(
                String.format("admin type %s not recognized", adminType.get()));
          }
        });
    defaultRedirectionActionBuilder(
        (ctx, store) ->
            Optional.of(
                HttpActionHelper.buildRedirectUrlAction(
                    ctx, routes.AdminProgramController.index().url())));
  }
}
