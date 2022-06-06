package auth.oidc.applicant;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.Roles;
import auth.oidc.OidcProfileAdapter;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.util.Locale;
import java.util.Objects;
import javax.inject.Provider;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.UserRepository;

/**
 * This class ensures that the OidcProfileCreator that both the AD and IDCS clients use will
 * generate a CiviFormProfile object. This is necessary for merging those accounts with existing
 * accounts - that's not usually needed in web applications which is why we have to write this class
 * - pac4j doesn't come with it. It's abstract because AD and IDCS need slightly different
 * implementations of the two abstract methods.
 */
public abstract class OidcApplicantProfileAdapter extends OidcProfileAdapter {

  protected final Config app_configuration;

  protected final String localeAttributeConfigName = "locale_attribute";
  protected final String firstNameAttributeConfigName = "first_name_attribute";
  protected final String secondNameAttributeConfigName = "second_name_attribute";
  protected final String emailAttributeConfigName = "email_attribute";

  public OidcApplicantProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      Config app_configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
    this.app_configuration = app_configuration;
  }

  /*
   * Provide the prefix used in the application.conf
   */
  protected abstract String attributePrefix();

  protected String getConfigurationValue(String attr) {
    return getConfigurationValue(attr, "");
  }

  protected String getConfigurationValue(String attr, String defaultValue) {
    if (app_configuration.hasPath(attr)) {
      return app_configuration.getString(attr);
    }
    return defaultValue;
  }

  protected String getEmailAttributeName() {
    return getConfigurationValue(attributePrefix() + "." + emailAttributeConfigName);
  }

  protected String getLocaleAttributeName() {
    return getConfigurationValue(attributePrefix() + "." + localeAttributeConfigName);
  }

  protected String getFirstNameAttributeName() {
    return getConfigurationValue(attributePrefix() + "." + firstNameAttributeConfigName);
  }

  protected String getSecondNameAttributeName() {
    return getConfigurationValue(attributePrefix() + "." + secondNameAttributeConfigName);
  }

  protected String getName(OidcProfile oidcProfile) {
    final String firstNameAttributeName = getFirstNameAttributeName();
    final String secondNameAttributeName = getSecondNameAttributeName();

    String firstName = "", secondName = "";
    if (!firstNameAttributeName.isBlank()) {
      firstName =
          Objects.toString(oidcProfile.getAttribute(firstNameAttributeName, String.class), "");
    }
    if (!secondNameAttributeName.isBlank()) {
      secondName =
          Objects.toString(oidcProfile.getAttribute(secondNameAttributeName, String.class), "");
    }
    if (!firstName.isBlank() && !secondName.isBlank()) {
      return String.format("%s %s", firstName, secondName);
    } else if (!firstName.isBlank()) {
      return firstName;
    }
    return secondName;
  }

  protected String getLocale(OidcProfile oidcProfile) {
    final String localeAttributeName = getLocaleAttributeName();

    if (!localeAttributeName.isBlank()) {
      return Objects.toString(oidcProfile.getAttribute(localeAttributeName, String.class), "");
    }
    return "";
  }

  @Override
  protected String emailAttributeName() {
    return getEmailAttributeName();
  }
  ;

  /** Create a totally new Applicant CiviForm profile informed by the provided OidcProfile. */
  @Override
  public CiviFormProfile createEmptyCiviFormProfile(OidcProfile profile) {
    return profileFactory.wrapProfileData(profileFactory.createNewApplicant());
  }

  protected boolean isTrustedIntermediary(CiviFormProfile profile) {
    return profile.getAccount().join().getMemberOfGroup().isPresent();
  }

  @Override
  protected ImmutableSet<Roles> roles(CiviFormProfile profile, OidcProfile oidcProfile) {
    if (isTrustedIntermediary(profile)) {
      return ImmutableSet.of(Roles.ROLE_APPLICANT, Roles.ROLE_TI);
    }
    return ImmutableSet.of(Roles.ROLE_APPLICANT);
  }

  @Override
  protected void adaptForRole(CiviFormProfile profile, ImmutableSet<Roles> roles) {
    // not needed
  }

  @Override
  protected void possiblyModifyConfigBasedOnCred(Credentials cred) {
    // not needed
  }

  /** Merge the two provided profiles into a new CiviFormProfileData. */
  @Override
  protected CiviFormProfileData mergeCiviFormProfile(
      CiviFormProfile civiformProfile, OidcProfile oidcProfile) {
    final String locale = getLocale(oidcProfile);
    final String name = getName(oidcProfile);

    if (!locale.isBlank() || !name.isBlank()) {
      civiformProfile
          .getApplicant()
          .thenApplyAsync(
              applicant -> {
                if (!locale.isBlank()) {
                  applicant.getApplicantData().setPreferredLocale(Locale.forLanguageTag(locale));
                }
                if (!name.isBlank()) {
                  applicant.getApplicantData().setUserName(name);
                }
                applicant.save();
                return null;
              })
          .toCompletableFuture()
          .join();
    }

    return super.mergeCiviFormProfile(civiformProfile, oidcProfile);
  }
}
