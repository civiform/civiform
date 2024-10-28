package auth.oidc;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.CiviFormProfileMerger;
import auth.IdentityProviderType;
import auth.ProfileFactory;
import auth.ProfileUtils;
import auth.Role;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.inject.Provider;
import models.ApplicantModel;
import org.apache.commons.lang3.NotImplementedException;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.creator.OidcProfileCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AccountRepository;
import services.settings.SettingsManifest;

/**
 * This class ensures that the OidcProfileCreator that both the AD and IDCS clients use will
 * generate a CiviFormProfile object. This is necessary for merging those accounts with existing
 * accounts - that's not usually needed in web applications which is why we have to write this class
 * - pac4j doesn't come with it. It's abstract because AD and IDCS need slightly different
 * implementations of the two abstract methods.
 */
public abstract class CiviformOidcProfileCreator extends OidcProfileCreator {
  private static final Logger LOGGER = LoggerFactory.getLogger(CiviformOidcProfileCreator.class);
  protected final ProfileFactory profileFactory;
  protected final Provider<AccountRepository> accountRepositoryProvider;
  protected final CiviFormProfileMerger civiFormProfileMerger;
  protected final SettingsManifest settingsManifest;

  public CiviformOidcProfileCreator(
      OidcConfiguration configuration, OidcClient client, OidcClientProviderParams params) {
    super(Preconditions.checkNotNull(configuration), Preconditions.checkNotNull(client));
    this.profileFactory = Preconditions.checkNotNull(params.profileFactory());
    this.accountRepositoryProvider = Preconditions.checkNotNull(params.accountRepositoryProvider());
    this.civiFormProfileMerger =
        new CiviFormProfileMerger(profileFactory, accountRepositoryProvider);
    this.settingsManifest =
        new SettingsManifest(Preconditions.checkNotNull(params.configuration()));
  }

  protected abstract String emailAttributeName();

  protected abstract ImmutableSet<Role> roles(CiviFormProfile profile, OidcProfile oidcProfile);

  protected abstract void adaptForRole(CiviFormProfile profile, ImmutableSet<Role> roles);

  /** Create a totally new CiviForm profile informed by the provided OidcProfile. */
  public abstract CiviFormProfile createEmptyCiviFormProfile(OidcProfile profile);

  /** Returns the type of the identity provider used to create profiles. */
  protected abstract IdentityProviderType identityProviderType();

  protected final Optional<String> getEmail(OidcProfile oidcProfile) {
    final String emailAttributeName = emailAttributeName();

    if (emailAttributeName.isBlank()) {
      return Optional.empty();
    }

    return Optional.ofNullable(oidcProfile.getAttribute(emailAttributeName, String.class));
  }

  private Optional<String> getAuthorityId(OidcProfile oidcProfile) {
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
      Optional<CiviFormProfile> maybeCiviFormProfile, OidcProfile oidcProfile, WebContext context) {
    var civiformProfile =
        maybeCiviFormProfile.orElseGet(
            () -> {
              LOGGER.debug("Found no existing profile in session cookie.");
              return createEmptyCiviFormProfile(oidcProfile);
            });
    return mergeCiviFormProfile(civiformProfile, oidcProfile, context);
  }

  /** Merge the two provided profiles into a new CiviFormProfileData. */
  protected CiviFormProfileData mergeCiviFormProfile(
      CiviFormProfile civiformProfile, OidcProfile oidcProfile, WebContext context) {

    // Meaning: whatever you signed in with most recently is the role you have.
    ImmutableSet<Role> roles = roles(civiformProfile, oidcProfile);
    roles.stream()
        .map(Role::toString)
        .forEach(role -> civiformProfile.getProfileData().addRole(role));
    adaptForRole(civiformProfile, roles);

    String emailAddress =
        getEmail(oidcProfile)
            .orElseThrow(
                () -> new InvalidOidcProfileException("Unable to get email from profile."));

    // If the civiformProfile is a trusted intermediary, bypass remaining merging because
    // we don't want to actually merge the guest profile into theirs.
    if (isTrustedIntermediary(civiformProfile)) {
      // Setting the email here ensures the canonical email field is populated
      // regardless of what the identity provider uses. See comment on
      // CiviFormProfileData.setEmail() for more info.
      return civiformProfile.getProfileData().setEmail(emailAddress);
    }

    civiformProfile.setEmailAddress(emailAddress).join();

    String authorityId =
        getAuthorityId(oidcProfile)
            .orElseThrow(
                () -> new InvalidOidcProfileException("Unable to get authority ID from profile."));

    civiformProfile.setAuthorityId(authorityId).join();

    civiformProfile.getProfileData().addAttribute(CommonProfileDefinition.EMAIL, emailAddress);

    if (enhancedLogoutEnabled()) {
      // Save the id_token from the returned OidcProfile in the account so that it can be
      // retrieved at logout time.
      civiformProfile
          .getAccount()
          .thenAccept(
              account -> {
                accountRepositoryProvider
                    .get()
                    .addIdTokenAndPrune(
                        account,
                        civiformProfile.getProfileData().getSessionId(),
                        oidcProfile.getIdTokenString());
              })
          .join();
    }

    return civiformProfile.getProfileData();
  }

  @Override
  public Optional<UserProfile> create(CallContext callContext, Credentials credentials) {
    ProfileUtils profileUtils = new ProfileUtils(callContext.sessionStore(), profileFactory);
    Optional<UserProfile> oidcProfile = super.create(callContext, credentials);

    if (oidcProfile.isEmpty()) {
      LOGGER.warn("Didn't get a valid profile back from OIDC.");
      return Optional.empty();
    }

    if (!(oidcProfile.get() instanceof OidcProfile)) {
      LOGGER.warn(
          "Got a profile from OIDC callback but it wasn't an OIDC profile: %s",
          oidcProfile.get().getClass().getName());
      return Optional.empty();
    }

    OidcProfile profile = (OidcProfile) oidcProfile.get();
    Optional<ApplicantModel> existingApplicant = getExistingApplicant(profile);
    Optional<CiviFormProfile> guestProfile =
        profileUtils.optionalCurrentUserProfile(callContext.webContext());

    // The merge function signature specifies the two profiles as parameters.
    // We need to supply an extra parameter (context), so bind it here.
    BiFunction<Optional<CiviFormProfile>, OidcProfile, UserProfile> mergeFunction =
        (cProfile, oProfile) ->
            this.mergeCiviFormProfile(cProfile, oProfile, callContext.webContext());
    return civiFormProfileMerger.mergeProfiles(
        existingApplicant, guestProfile, profile, mergeFunction);
  }

  @VisibleForTesting
  public final Optional<ApplicantModel> getExistingApplicant(OidcProfile profile) {
    // User keying changed in March 2022 and is reflected and managed here.
    // Originally users were keyed on their email address, however this is not
    // guaranteed to be a unique stable ID. In March 2022 the code base changed to
    // using authority_id which is unique and stable per authentication provider.

    String authorityId =
        getAuthorityId(profile)
            .orElseThrow(
                () -> new InvalidOidcProfileException("Unable to get authority ID from profile."));

    Optional<ApplicantModel> applicantOpt =
        accountRepositoryProvider
            .get()
            .lookupApplicantByAuthorityId(authorityId)
            .toCompletableFuture()
            .join();
    if (applicantOpt.isPresent()) {
      LOGGER.debug("Found user using authority ID: {}", authorityId);
      return applicantOpt;
    }

    // For pre-existing deployments before April 2022, users will exist without an
    // authority ID and will be keyed on their email.
    String userEmail = profile.getAttribute(emailAttributeName(), String.class);
    LOGGER.debug("Looking up user using email {}", userEmail);
    return accountRepositoryProvider
        .get()
        .lookupApplicantByEmail(userEmail)
        .toCompletableFuture()
        .join();
  }

  protected final boolean isTrustedIntermediary(CiviFormProfile profile) {
    return profile.getAccount().join().getMemberOfGroup().isPresent();
  }

  private boolean enhancedLogoutEnabled() {
    // Sigh. This would be much nicer with switch expressions (Java 12) and exhaustive switch (Java
    // 17).
    switch (identityProviderType()) {
      case ADMIN_IDENTITY_PROVIDER:
        return settingsManifest.getAdminOidcEnhancedLogoutEnabled();
      case APPLICANT_IDENTITY_PROVIDER:
        return settingsManifest.getApplicantOidcEnhancedLogoutEnabled();
      default:
        throw new NotImplementedException(
            "Identity provider type not handled: " + identityProviderType());
    }
  }
}
