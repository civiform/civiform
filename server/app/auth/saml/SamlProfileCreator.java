package auth.saml;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.CiviFormProfileMerger;
import auth.ProfileFactory;
import auth.ProfileUtils;
import auth.Role;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import javax.inject.Provider;
import models.Applicant;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.creator.AuthenticatorProfileCreator;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;

public class SamlProfileCreator extends AuthenticatorProfileCreator {

  private static final Logger logger = LoggerFactory.getLogger(SamlProfileCreator.class);
  protected final CiviFormProfileMerger civiFormProfileMerger;
  protected final ProfileFactory profileFactory;
  protected final Provider<UserRepository> applicantRepositoryProvider;
  protected final SAML2Configuration saml2Configuration;
  // TODO(#3856): Update with a non deprecated saml impl.
  @SuppressWarnings("deprecation")
  protected final SAML2Client saml2Client;

  // TODO(#3856): Update with a non deprecated saml impl.
  @SuppressWarnings("deprecation")
  public SamlProfileCreator(
      SAML2Configuration configuration,
      SAML2Client client,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super();
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
    this.applicantRepositoryProvider = Preconditions.checkNotNull(applicantRepositoryProvider);
    this.civiFormProfileMerger =
        new CiviFormProfileMerger(profileFactory, applicantRepositoryProvider);
    this.saml2Client = client;
    this.saml2Configuration = configuration;
  }

  @Override
  public Optional<UserProfile> create(
      Credentials cred, WebContext context, SessionStore sessionStore) {
    ProfileUtils profileUtils = new ProfileUtils(sessionStore, profileFactory);

    Optional<UserProfile> samlProfile = super.create(cred, context, sessionStore);

    if (samlProfile.isEmpty()) {
      logger.warn("Didn't get a valid profile back from SAML");
      return Optional.empty();
    }

    if (!(samlProfile.get() instanceof SAML2Profile)) {
      logger.warn(
          "Got a profile from SAML2 callback but it wasn't a SAML profile: %s",
          samlProfile.get().getClass().getName());
      return Optional.empty();
    }

    SAML2Profile profile = (SAML2Profile) samlProfile.get();
    Optional<Applicant> existingApplicant = getExistingApplicant(profile);
    Optional<CiviFormProfile> guestProfile = profileUtils.currentUserProfile(context);
    return civiFormProfileMerger.mergeProfiles(
        existingApplicant,
        guestProfile,
        /* idToken = */ Optional.empty(),
        profile,
        this::mergeCiviFormProfile);
  }

  @VisibleForTesting
  Optional<Applicant> getExistingApplicant(SAML2Profile profile) {
    // authority_id is used as the unique stable key for users. This is unique and
    // stable per
    // authentication provider.
    String authorityId =
        getAuthorityId(profile)
            .orElseThrow(
                () -> new InvalidSamlProfileException("Unable to get authority ID from profile."));

    return applicantRepositoryProvider
        .get()
        .lookupApplicantByAuthorityId(authorityId)
        .toCompletableFuture()
        .join();
  }

  protected Optional<String> getAuthorityId(SAML2Profile profile) {
    // In SAML the user is uniquely identified by the issuer and subject claims.
    // https://docs.oasis-open.org/security/saml-subject-id-attr/v1.0/cs01/saml-subject-id-attr-v1.0-cs01.html#_Toc536097226
    //
    // We combine the two to create the unique authority id.
    String issuer = profile.getIssuerEntityID();
    // Subject in SAML is the NameID. It identifies the specific user in the issuer.
    // Pac4j treats the subject as special, and you can't simply ask for the "sub"
    // claim.
    String subject = profile.getId();
    if (issuer == null || subject == null) {
      return Optional.empty();
    }
    // This string format can never change. It is the unique ID for OIDC based
    // account.
    return Optional.of(String.format("Issuer: %s NameID: %s", issuer, subject));
  }

  public CiviFormProfile createEmptyCiviformProfile() {
    return profileFactory.wrapProfileData(profileFactory.createNewApplicant());
  }

  @VisibleForTesting
  public CiviFormProfileData mergeCiviFormProfile(
      Optional<CiviFormProfile> maybeCiviFormProfile, SAML2Profile saml2Profile) {
    var civiFormProfile =
        maybeCiviFormProfile.orElseGet(
            () -> {
              logger.debug("Found no existing profile in session cookie.");
              return createEmptyCiviformProfile();
            });
    String authorityId =
        getAuthorityId(saml2Profile)
            .orElseThrow(
                () -> new InvalidSamlProfileException("Unable to get authority ID from profile"));
    civiFormProfile.setAuthorityId(authorityId).join();

    final String locale = saml2Profile.getAttribute("locale", String.class);
    final boolean hasLocale = !Strings.isNullOrEmpty(locale);

    final String firstName = saml2Profile.getAttribute("first_name", String.class);
    final boolean hasFirstName = !Strings.isNullOrEmpty(firstName);

    // TODO: figure out why the last_name attribute is being returned as an
    // ArrayList.
    final String lastName = extractAttributeFromArrayList(saml2Profile, "last_name");
    final boolean hasLastName = !Strings.isNullOrEmpty(lastName);

    if (hasLocale || hasFirstName || hasLastName) {
      civiFormProfile
          .getApplicant()
          .thenApplyAsync(
              applicant -> {
                if (hasLocale) {
                  applicant.getApplicantData().setPreferredLocale(Locale.forLanguageTag(locale));
                }
                if (hasFirstName && hasLastName) {
                  applicant
                      .getApplicantData()
                      .setUserName(String.format("%s %s", firstName, lastName));
                } else if (hasFirstName) {
                  applicant.getApplicantData().setUserName(firstName);
                } else if (hasLastName) {
                  applicant.getApplicantData().setUserName(lastName);
                }
                applicant.save();
                return null;
              })
          .toCompletableFuture()
          .join();
    }
    String emailAddress = saml2Profile.getEmail();
    civiFormProfile.setEmailAddress(emailAddress).join();

    civiFormProfile.getProfileData().addAttribute(CommonProfileDefinition.EMAIL, emailAddress);
    // Meaning: whatever you signed in with most recently is the role you have.
    ImmutableSet<Role> roles = roles(civiFormProfile);
    for (Role role : roles) {
      civiFormProfile.getProfileData().addRole(role.toString());
    }
    return civiFormProfile.getProfileData();
  }

  private String extractAttributeFromArrayList(SAML2Profile profile, String attr) {
    Optional<ArrayList> attributeArray =
        Optional.ofNullable(profile.getAttribute(attr, ArrayList.class));
    if (attributeArray.isEmpty()) {
      return "";
    }
    StringJoiner sj = new StringJoiner(" ");
    for (Object s : attributeArray.get()) {
      sj.add((String) s);
    }
    return sj.toString();
  }

  protected ImmutableSet<Role> roles(CiviFormProfile profile) {
    if (profile.getAccount().join().getMemberOfGroup().isPresent()) {
      return ImmutableSet.of(Role.ROLE_APPLICANT, Role.ROLE_TI);
    }
    return ImmutableSet.of(Role.ROLE_APPLICANT);
  }
}
