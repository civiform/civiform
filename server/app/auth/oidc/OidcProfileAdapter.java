package auth.oidc;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.CiviFormProfileMerger;
import auth.ProfileFactory;
import auth.ProfileUtils;
import auth.Roles;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Provider;
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
public abstract class OidcProfileAdapter extends OidcProfileCreator {

  private static final Logger logger = LoggerFactory.getLogger(OidcProfileAdapter.class);
  protected final ProfileFactory profileFactory;
  protected final Provider<UserRepository> applicantRepositoryProvider;
  protected final CiviFormProfileMerger civiFormProfileMerger;

  public OidcProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, client);
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
    this.applicantRepositoryProvider = applicantRepositoryProvider;
    this.civiFormProfileMerger =
        new CiviFormProfileMerger(profileFactory, applicantRepositoryProvider);
  }

  protected abstract String emailAttributeName();

  protected abstract ImmutableSet<Roles> roles(CiviFormProfile profile, OidcProfile oidcProfile);

  protected abstract void adaptForRole(CiviFormProfile profile, ImmutableSet<Roles> roles);

  /** Create a totally new CiviForm profile informed by the provided OidcProfile. */
  public abstract CiviFormProfile createEmptyCiviFormProfile(OidcProfile profile);

  protected Optional<String> getEmail(OidcProfile oidcProfile) {
    final String emailAttributeName = emailAttributeName();

    if (!emailAttributeName.isBlank()) {
      return Optional.ofNullable(oidcProfile.getAttribute(emailAttributeName, String.class));
    }
    return Optional.empty();
  }

  protected Optional<String> getAuthorityId(OidcProfile oidcProfile) {
    // In OIDC the user is uniquely identified by the iss(user) and sub(ject)
    // claims.
    // https://openid.net/specs/openid-connect-core-1_0.html#IDToken
    //
    // We combine the two to create the unique authority id.
    // Issuer is necessary as CiviForm has different authentication systems for
    // Admins and Applicants.
    String issuer = oidcProfile.getAttribute("iss", String.class);
    // Subject identifies the specific user in the issuer.
    // Pac4j treats the subject as special, and you can't simply ask for the "sub"
    // claim.
    String subject = oidcProfile.getId();
    if (issuer == null || subject == null) {
      return Optional.empty();
    }
    // This string format can never change. It is the unique ID for OIDC based
    // account.
    return Optional.of(String.format("iss: %s sub: %s", issuer, subject));
  }

  /**
   * Merge the two provided profiles into a new CiviformProfileData, making sure to create a new
   * civiFormProfile if it doesn't already exist.
   */
  @VisibleForTesting
  public CiviFormProfileData mergeCiviFormProfile(
      Optional<CiviFormProfile> maybeCiviFormProfile, OidcProfile oidcProfile) {
    var civiformProfile =
        maybeCiviFormProfile.orElseGet(
            () -> {
              logger.debug("Found no existing profile in session cookie.");
              return createEmptyCiviFormProfile(oidcProfile);
            });
    return mergeCiviFormProfile(civiformProfile, oidcProfile);
  }

  /** Merge the two provided profiles into a new CiviFormProfileData. */
  protected CiviFormProfileData mergeCiviFormProfile(
      CiviFormProfile civiformProfile, OidcProfile oidcProfile) {
    String emailAddress =
        getEmail(oidcProfile)
            .orElseThrow(
                () -> new InvalidOidcProfileException("Unable to get email from profile."));
    civiformProfile.setEmailAddress(emailAddress).join();

    String authorityId =
        getAuthorityId(oidcProfile)
            .orElseThrow(
                () -> new InvalidOidcProfileException("Unable to get authority ID from profile."));

    civiformProfile.setAuthorityId(authorityId).join();

    civiformProfile.getProfileData().addAttribute(CommonProfileDefinition.EMAIL, emailAddress);

    // Meaning: whatever you signed in with most recently is the role you have.
    ImmutableSet<Roles> roles = roles(civiformProfile, oidcProfile);
    roles.stream()
        .map(Roles::toString)
        .forEach(role -> civiformProfile.getProfileData().addRole(role));
    adaptForRole(civiformProfile, roles);
    return civiformProfile.getProfileData();
  }

  @Override
  public Optional<UserProfile> create(
      Credentials cred, WebContext context, SessionStore sessionStore) {
    ProfileUtils profileUtils = new ProfileUtils(sessionStore, profileFactory);
    possiblyModifyConfigBasedOnCred(cred);
    Optional<UserProfile> oidcProfile = super.create(cred, context, sessionStore);

    if (oidcProfile.isEmpty()) {
      logger.warn("Didn't get a valid profile back from OIDC.");
      return Optional.empty();
    }

    if (!(oidcProfile.get() instanceof OidcProfile)) {
      logger.warn(
          "Got a profile from OIDC callback but it wasn't an OIDC profile: %s",
          oidcProfile.get().getClass().getName());
      return Optional.empty();
    }

    OidcProfile profile = (OidcProfile) oidcProfile.get();
    Optional<Applicant> existingApplicant = getExistingApplicant(profile);
    Optional<CiviFormProfile> existingProfile = profileUtils.currentUserProfile(context);
    return civiFormProfileMerger.mergeProfiles(
        existingApplicant, existingProfile, profile, this::mergeCiviFormProfile);
  }

  @VisibleForTesting
  Optional<Applicant> getExistingApplicant(OidcProfile profile) {
    // User keying changed in March 2022 and is reflected and managed here.
    // Originally users were keyed on their email address, however this is not
    // guaranteed to be a unique stable ID. In March 2022 the code base changed to
    // using authority_id which is unique and stable per authentication provider.

    String authorityId =
        getAuthorityId(profile)
            .orElseThrow(
                () -> new InvalidOidcProfileException("Unable to get authority ID from profile."));

    Optional<Applicant> applicantOpt =
        applicantRepositoryProvider
            .get()
            .lookupApplicantByAuthorityId(authorityId)
            .toCompletableFuture()
            .join();
    if (applicantOpt.isPresent()) {
      logger.debug("Found user using authority ID: {}", authorityId);
      return applicantOpt;
    }

    // For pre-existing deployments before April 2022, users will exist without an
    // authority ID and will be keyed on their email.
    String userEmail = profile.getAttribute(emailAttributeName(), String.class);
    logger.debug("Looking up user using email {}", userEmail);
    return applicantRepositoryProvider
        .get()
        .lookupApplicantByEmail(userEmail)
        .toCompletableFuture()
        .join();
  }

  protected abstract void possiblyModifyConfigBasedOnCred(Credentials cred);
}
