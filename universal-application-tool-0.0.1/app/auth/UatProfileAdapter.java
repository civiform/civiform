package auth;

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

public abstract class UatProfileAdapter extends OidcProfileCreator {
    protected ProfileFactory profileFactory;

    private static Logger log = LoggerFactory.getLogger(UatProfileAdapter.class);

    public UatProfileAdapter(OidcConfiguration configuration, OidcClient client, ProfileFactory profileFactory) {
        super(configuration, client);
        this.profileFactory = profileFactory;
    }

    public abstract UatProfileData uatProfileFromOidcProfile(OidcProfile profile);

    public abstract UatProfileData mergeUatProfile(UatProfile uatProfile, OidcProfile oidcProfile);

    @Override
    public Optional<UserProfile> create(Credentials cred, WebContext context, SessionStore sessionStore) {
        ProfileUtils profileUtils = new ProfileUtils(sessionStore, profileFactory);
        Optional<UatProfile> existingProfile = profileUtils.currentUserProfile(context);
        Optional<UserProfile> oidcProfile = super.create(cred, context, sessionStore);
        if (oidcProfile.isEmpty()) {
            log.debug("Didn't get a valid profile back from OIDC.");
            return Optional.empty();
        }
        if (!(oidcProfile.get() instanceof OidcProfile)) {
            log.warn("Got a profile from OIDC callback but it wasn't an OIDC profile: %s", oidcProfile.get());
            return Optional.empty();
        }
        OidcProfile profile = (OidcProfile) oidcProfile.get();
        if (existingProfile.isEmpty()) {
            log.debug("Found no existing profile in session cookie.");
            return Optional.of(uatProfileFromOidcProfile(profile));
        } else {
            return Optional.of(mergeUatProfile(existingProfile.get(), profile));
        }
    }
}
