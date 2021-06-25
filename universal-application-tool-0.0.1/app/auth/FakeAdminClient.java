package auth;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import controllers.admin.routes;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.credentials.AnonymousCredentials;
import org.pac4j.core.util.HttpActionHelper;

/**
 * This class implements a special client that allows logging in without a real AD account. The
 * feature is only enabled in development environment.
 */
public class FakeAdminClient extends IndirectClient {

  public static final String CLIENT_NAME = "FakeAdminClient";
  public static final String GLOBAL_ADMIN = "GLOBAL";
  public static final String PROGRAM_ADMIN = "PROGRAM";

  public static final ImmutableSet<String> ACCEPTED_HOSTS =
      ImmutableSet.of("localhost", "civiform", "staging.seattle.civiform.com");

  private ProfileFactory profileFactory;

  public static boolean canEnable(String host) {
    return FakeAdminClient.ACCEPTED_HOSTS.stream()
        .anyMatch(acceptedHost -> host.equals(acceptedHost) || host.startsWith(acceptedHost + ":"));
  }

  @Inject
  public FakeAdminClient(ProfileFactory profileFactory) {
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
  }

  @Override
  protected void internalInit() {
    defaultCredentialsExtractor(
        (ctx, store) -> {
          // Double check that we haven't been fooled into allowing this somehow.
          if (!ACCEPTED_HOSTS.contains(ctx.getServerName())) {
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
