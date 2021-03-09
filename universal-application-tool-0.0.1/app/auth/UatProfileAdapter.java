package auth;

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.creator.OidcProfileCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class ensures that the OidcProfileCreator that both the AD and IDCS clients use will
 * generate a UatProfile object. This is necessary for merging those accounts with existing accounts
 * - that's not usually needed in web applications which is why we have to write this class - pac4j
 * doesn't come with it. It's abstract because AD and IDCS need slightly different implementations
 * of the two abstract methods.
 */
public abstract class UatProfileAdapter extends OidcProfileCreator {
  protected final ProfileFactory profileFactory;

  private static Logger LOG = LoggerFactory.getLogger(UatProfileAdapter.class);

  public UatProfileAdapter(
      OidcConfiguration configuration, OidcClient client, ProfileFactory profileFactory) {
    super(configuration, client);
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
  }

  /** Create a totally new UAT profile from the provided OidcProfile. */
  public abstract UatProfileData uatProfileFromOidcProfile(OidcProfile profile);

  /** Merge the two provided profiles into a new UatProfileData. */
  public abstract UatProfileData mergeUatProfile(UatProfile uatProfile, OidcProfile oidcProfile);

  @Override
  public Optional<UserProfile> create(
      Credentials cred, WebContext context, SessionStore sessionStore) {
    ProfileUtils profileUtils = new ProfileUtils(sessionStore, profileFactory);
    Optional<UatProfile> existingProfile = profileUtils.currentUserProfile(context);
    Optional<UserProfile> oidcProfile = super.create(cred, context, sessionStore);

    if (oidcProfile.isEmpty()) {
      LOG.warn("Didn't get a valid profile back from OIDC.");
      return Optional.empty();
    }

    if (!(oidcProfile.get() instanceof OidcProfile)) {
      LOG.warn(
          "Got a profile from OIDC callback but it wasn't an OIDC profile: %s", oidcProfile.get());
      return Optional.empty();
    }

    OidcProfile profile = (OidcProfile) oidcProfile.get();
    if (existingProfile.isEmpty()) {
      LOG.debug("Found no existing profile in session cookie.");
      return Optional.of(uatProfileFromOidcProfile(profile));
    } else {
      return Optional.of(mergeUatProfile(existingProfile.get(), profile));
    }
  }
}
