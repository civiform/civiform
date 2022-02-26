package auth;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
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

  private static Logger LOG = LoggerFactory.getLogger(SamlCiviFormProfileAdapter.class);
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
    this.applicantRepositoryProvider = applicantRepositoryProvider;
    this.saml2Client = client;
    this.saml2Configuration = configuration;
  }

  @Override
  public Optional<UserProfile> create(
      Credentials cred, WebContext context, SessionStore sessionStore) {
    ProfileUtils profileUtils = new ProfileUtils(sessionStore, profileFactory);

    Optional<UserProfile> samlProfile = super.create(cred, context, sessionStore);

    if (samlProfile.isEmpty()) {
      LOG.warn("Didn't get a valid profile back from SAML");
      return Optional.empty();
    }

    if (!(samlProfile.get() instanceof SAML2Profile)) {
      LOG.warn(
          "Got a profile from SAML2 callback but it wasn't a SAML profile: %s", samlProfile.get());
      return Optional.empty();
    }

    SAML2Profile profile = (SAML2Profile) samlProfile.get();

    // Check if we already have a profile in the database for the user returned to us by SAML2
    Optional<Applicant> existingApplicant =
        applicantRepositoryProvider
            .get()
            .lookupApplicant(profile.getAttribute("email", String.class))
            .toCompletableFuture()
            .join();

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
      LOG.debug("Found no existing profile in session cookie.");
      return Optional.of(civiformProfileFromSamlProfile(profile));
    } else {
      return Optional.of(mergeCiviFormProfile(existingProfile.get(), profile));
    }
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

    // TODO: figure out why the last_name attribute is being returned as an ArrayList because this
    // feels like it shouldn't be necessary.
    final ArrayList lastNameArray = saml2Profile.getAttribute("last_name", ArrayList.class);
    StringBuilder sb = new StringBuilder();
    for (Object s : lastNameArray) {
      sb.append(s);
    }
    String lastName = sb.toString();
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
    ImmutableSet<Roles> roles = roles(civiFormProfile);
    for (Roles role : roles) {
      civiFormProfile.getProfileData().addRole(role.toString());
    }
    return civiFormProfile.getProfileData();
  }

  protected ImmutableSet<Roles> roles(CiviFormProfile profile) {
    if (profile.getAccount().join().getMemberOfGroup().isPresent()) {
      return ImmutableSet.of(Roles.ROLE_APPLICANT, Roles.ROLE_TI);
    }
    return ImmutableSet.of(Roles.ROLE_APPLICANT);
  }
}
