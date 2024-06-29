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
 * account. The feature is only enabled in demo mode.
 */
public class FakeAdminClient extends IndirectClient {

  public static final String CLIENT_NAME = "FakeAdminClient";
  public static final String GLOBAL_ADMIN = "GLOBAL";
  public static final String PROGRAM_ADMIN = "PROGRAM";
  public static final String DUAL_ADMIN = "DUAL";
  public static final String TRUSTED_INTERMEDIARY = "TRUSTED_INTERMEDIARY";

  private final ImmutableSet<String> acceptedHosts;
  private final ProfileFactory profileFactory;

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

  // forceReinit is a variable added in Pac4j 5.4.0 seen here:
  // https://github.com/pac4j/pac4j/commit/8b8ad4ddfaa6525804d5b94383dfb292a8da5622
  // It sets whether the object should be reinitialized. We do not need to set it ourselves as the
  // value
  // is being handled by the parent class.
  @Override
  protected void internalInit(final boolean forceReinit) {
    // setCredentialsExtractorIfUndefined(ctx -> {
    setCredentialsExtractor(
        ctx -> {
          // Double check that we haven't been fooled into allowing this somehow.
          if (!acceptedHosts.contains(ctx.webContext().getServerName())) {
            throw new UnsupportedOperationException(
                "You cannot create a fake admin unless you are running locally.");
          }
          return Optional.of(new AnonymousCredentials());
        });

    // setAuthenticatorIfUndefined(
    setAuthenticator(
        (ctx, cred) -> {
          Optional<String> adminType = ctx.webContext().getRequestParameter("adminType");
          if (adminType.isEmpty()) {
            throw new IllegalArgumentException("no admin type provided.");
          }
          if (adminType.get().equals(GLOBAL_ADMIN)) {
            cred.setUserProfile(profileFactory.createNewFakeAdmin());
          } else if (adminType.get().equals(PROGRAM_ADMIN)) {
            cred.setUserProfile(profileFactory.createFakeProgramAdmin());
          } else if (adminType.get().equals(DUAL_ADMIN)) {
            cred.setUserProfile(profileFactory.createFakeDualAdmin());
          } else if (adminType.get().equals(TRUSTED_INTERMEDIARY)) {
            cred.setUserProfile(profileFactory.createFakeTrustedIntermediary());
          } else {
            throw new IllegalArgumentException(
                String.format("admin type %s not recognized", adminType.get()));
          }

          return Optional.of(cred);
        });

    // setRedirectionActionBuilderIfUndefined(
    setRedirectionActionBuilder(
        (ctx) ->
            Optional.of(
                HttpActionHelper.buildRedirectUrlAction(
                    ctx.webContext(), routes.AdminProgramController.index().url())));
  }
}
