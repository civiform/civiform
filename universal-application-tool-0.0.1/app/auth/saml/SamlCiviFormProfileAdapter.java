package auth.saml;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import auth.Roles;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import javax.inject.Provider;
import models.Account;
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

public class SamlCiviFormProfileAdapter extends AuthenticatorProfileCreator {

  private static final Logger logger = LoggerFactory.getLogger(SamlCiviFormProfileAdapter.class);
  protected final ProfileFactory profileFactory;
  protected final Provider<UserRepository> applicantRepositoryProvider;
  protected final SAML2Configuration saml2Configuration;
  protected SAML2Client saml2Client;

  public SamlCiviFormProfileAdapter(
      SAML2Configuration configuration,
      SAML2Client client,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super();
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
    this.applicantRepositoryProvider = Preconditions.checkNotNull(applicantRepositoryProvider);
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

    // Check if we already have a profile in the database for the user returned to us by SAML2.
    Optional<Applicant> existingApplicant = getExistingApplicant(profile);

    // Now we have a three-way merge situation.  We might have
    // 1) an applicant in the database (`existingApplicant`),
    // 2) a guest profile in the browser cookie (`existingProfile`)
    // 3) a SAML2Profile in the callback from the Identity Provider (`profile`).
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

    // Now merge in the information sent to us by the SAML Identity Provider.
    if (existingProfile.isEmpty()) {
      logger.debug("Found no existing profile in session cookie.");
      return Optional.of(civiformProfileFromSamlProfile(profile));
    }
    return Optional.of(mergeCiviFormProfile(existingProfile.get(), profile));
  }

  @VisibleForTesting
  Optional<Applicant> getExistingApplicant(SAML2Profile profile) {
    // authority_id is used as the unique stable key for users. This is unique and stable per
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
    // Wee combine the two to create the unique authority id.
    String issuer = profile.getIssuerEntityID();
    // Subject identifies the specific user in the issuer.
    // Pac4j treats the subject as special, and you can't simply ask for the "sub" claim.
    String subject = profile.getId();
    if (issuer == null || subject == null) {
      return Optional.empty();
    }
    // This string format can never change. It is the unique ID for OIDC based account.
    return Optional.of(String.format("iss: %s sub: %s", issuer, subject));
  }

  public CiviFormProfileData civiformProfileFromSamlProfile(SAML2Profile profile) {
    return mergeCiviFormProfile(
        profileFactory.wrapProfileData(profileFactory.createNewApplicant()), profile);
  }

  public CiviFormProfileData mergeCiviFormProfile(
      CiviFormProfile civiFormProfile, SAML2Profile saml2Profile) {
    final String locale = saml2Profile.getAttribute("locale", String.class);
    final boolean hasLocale = !Strings.isNullOrEmpty(locale);

    final String firstName = saml2Profile.getAttribute("first_name", String.class);
    final boolean hasFirstName = !Strings.isNullOrEmpty(firstName);

    // TODO: figure out why the last_name attribute is being returned as an ArrayList.
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

    Optional<String> authorityId = getAuthorityId(saml2Profile);
    if (authorityId.isEmpty()) {
      throw new InvalidSamlProfileException("Unable to get authority ID from profile.");
    }
    civiFormProfile.setAuthorityId(authorityId.get()).join();

    civiFormProfile.getProfileData().addAttribute(CommonProfileDefinition.EMAIL, emailAddress);
    // Meaning: whatever you signed in with most recently is the role you have.
    ImmutableSet<Roles> roles = roles(civiFormProfile);
    for (Roles role : roles) {
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

  protected ImmutableSet<Roles> roles(CiviFormProfile profile) {
    if (profile.getAccount().join().getMemberOfGroup().isPresent()) {
      return ImmutableSet.of(Roles.ROLE_APPLICANT, Roles.ROLE_TI);
    }
    return ImmutableSet.of(Roles.ROLE_APPLICANT);
  }
}
