package auth.oidc;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import auth.Roles;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.creator.OidcProfileCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;

/**
 * This class ensures that the OidcProfileCreator that both the AD and IDCS clients use will
 * generate a CiviFormProfile object. This is necessary for merging those accounts with existing
 * accounts - that's not usually needed in web applications which is why we have to write this class
 * - pac4j doesn't come with it. It's abstract because AD and IDCS need slightly different
 * implementations of the two abstract methods.
 */
public abstract class OidcCiviFormProfileAdapter extends OidcProfileCreator {
  private static final Logger logger = LoggerFactory.getLogger(OidcCiviFormProfileAdapter.class);
  protected final ProfileFactory profileFactory;
  protected final Provider<UserRepository> applicantRepositoryProvider;

  public OidcCiviFormProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, client);
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
    this.applicantRepositoryProvider = applicantRepositoryProvider;
  }

  protected abstract String emailAttributeName();

  protected abstract ImmutableSet<Roles> roles(CiviFormProfile profile, OidcProfile oidcProfile);

  protected abstract void adaptForRole(CiviFormProfile profile, ImmutableSet<Roles> roles);

  protected Optional<String> getAuthorityId(OidcProfile oidcProfile) {
    // In OIDC the user is uniquely identified by the iss(user) and sub(ject) claims.
    // We combine the two to create the unique authority id.
    // Issuer is unintuitively necessary as CiviForm has different authentication systems for Admins
    // and Applicants.
    String issuer = oidcProfile.getAttribute("iss", String.class);
    // Pac4j treats the subject as special, and you can't simply ask for the "sub" claim.
    String subject = oidcProfile.getId();
    // This should throw an error but this allows us to unit test for pre-existing Accounts without
    // an authority_id.
    // TODO(#2059): remove null allowance after Seattle data cleanup.
    if (issuer == null || subject == null) {
      return Optional.empty();
    }
    // This string format can never change. It is the unique ID for OIDC based account.
    return Optional.of(String.format("iss: %s sub: %s", issuer, subject));
  }

  /** Merge the two provided profiles into a new CiviFormProfileData. */
  public CiviFormProfileData mergeCiviFormProfile(
      CiviFormProfile civiformProfile, OidcProfile oidcProfile) {
    String emailAddress = oidcProfile.getAttribute(emailAttributeName(), String.class);
    Optional<String> authorityId = getAuthorityId(oidcProfile);
    civiformProfile.setEmailAddress(emailAddress).join();
    // This allows us to unit test for pre-existing Accounts without an authority_id.
    // TODO(#2059): remove optional allowance after Seattle data cleanup.
    if (authorityId.isPresent()) {
      civiformProfile.setAuthorityId(authorityId.get()).join();
    }
    civiformProfile.getProfileData().addAttribute(CommonProfileDefinition.EMAIL, emailAddress);
    // Meaning: whatever you signed in with most recently is the role you have.
    ImmutableSet<Roles> roles = roles(civiformProfile, oidcProfile);
    for (Roles role : roles) {
      civiformProfile.getProfileData().addRole(role.toString());
    }
    adaptForRole(civiformProfile, roles);
    return civiformProfile.getProfileData();
  }

  /** Create a totally new CiviForm profile from the provided OidcProfile. */
  public abstract CiviFormProfileData civiformProfileFromOidcProfile(OidcProfile oidcProfile);

  @Override
  public Optional<UserProfile> create(
      Credentials cred, WebContext context, SessionStore sessionStore) {
    logger.info("create");
    ProfileUtils profileUtils = new ProfileUtils(sessionStore, profileFactory);
    possiblyModifyConfigBasedOnCred(cred);
    Optional<UserProfile> oidcProfile = super.create(cred, context, sessionStore);

    if (oidcProfile.isEmpty()) {
      logger.warn("Didn't get a valid profile back from OIDC.");
      return Optional.empty();
    }

    logger.info("oidcProfile: {}", oidcProfile.get());

    if (!(oidcProfile.get() instanceof OidcProfile)) {
      logger.warn(
          "Got a profile from OIDC callback but it wasn't an OIDC profile: %s",
          oidcProfile.get().getClass().getName());
      return Optional.empty();
    }

    OidcProfile profile = (OidcProfile) oidcProfile.get();
    // Check if we already have a profile in the database for the user returned to us by OIDC.
    Optional<Applicant> existingApplicant =
        applicantRepositoryProvider
            .get()
            .lookupApplicant(profile.getAttribute(emailAttributeName(), String.class))
            .toCompletableFuture()
            .join();

    // Now we have a three-way merge situation.  We might have
    // 1) an applicant in the database (`existingApplicant`),
    // 2) a guest profile in the browser cookie (`existingProfile`)
    // 3) an OIDC account in the callback from the OIDC server (`profile`).
    // We will merge 1 and 2, if present, into `existingProfile`, then merge in `profile`.

    Optional<CiviFormProfile> existingProfile = profileUtils.currentUserProfile(context);
    if (existingApplicant.isPresent()) {
      if (existingProfile.isEmpty()) {
        // Easy merge case - we have an existing applicant, but no guest profile.
        // This will be the most common.
        existingProfile = Optional.of(profileFactory.wrap(existingApplicant.get()));
      } else {
        // Merge the two applicants and prefer the newer one.
        // For account, use the existing account and ignore the guest account.
        Applicant guestApplicant = existingProfile.get().getApplicant().join();
        Account existingAccount = existingApplicant.get().getAccount();
        Applicant mergedApplicant =
            applicantRepositoryProvider
                .get()
                .mergeApplicants(guestApplicant, existingApplicant.get(), existingAccount)
                .toCompletableFuture()
                .join();
        existingProfile = Optional.of(profileFactory.wrap(mergedApplicant));
      }
    }

    // Now merge in the information sent to us by the OIDC server.
    if (existingProfile.isEmpty()) {
      logger.debug("Found no existing profile in session cookie.");
      return Optional.of(civiformProfileFromOidcProfile(profile));
    } else {
      return Optional.of(mergeCiviFormProfile(existingProfile.get(), profile));
    }
  }

  protected abstract void possiblyModifyConfigBasedOnCred(Credentials cred);
}
