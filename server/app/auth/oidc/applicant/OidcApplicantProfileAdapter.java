package auth.oidc.applicant;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.Roles;
import auth.oidc.OidcProfileAdapter;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.util.Locale;
import javax.inject.Provider;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.UserRepository;
import java.util.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.beust.jcommander.internal.Nullable;
import com.google.common.base.Preconditions;
import java.util.stream.Collectors;

/**
 * This class ensures that the OidcProfileCreator that both the AD and IDCS clients use will
 * generate a CiviFormProfile object. This is necessary for merging those accounts with existing
 * accounts - that's not usually needed in web applications which is why we have to write this class
 * - pac4j doesn't come with it. It's abstract because AD and IDCS need slightly different
 * implementations of the two abstract methods.
 */
public abstract class OidcApplicantProfileAdapter extends OidcProfileAdapter {
  private final String emailAttributeName;
  private final Optional<String> localeAttributeName;
  private final ImmutableList<String> nameAttributeNames;

  public OidcApplicantProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider,
      String emailAttributeName,
      @Nullable String localeAttributeName,
      ImmutableList<String> nameAttributeNames) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
    this.emailAttributeName = Preconditions.checkNotNull(emailAttributeName);
    this.localeAttributeName = Optional.ofNullable(localeAttributeName);
    this.nameAttributeNames = Preconditions.checkNotNull(nameAttributeNames);
  }

  protected Optional<String> getName(OidcProfile oidcProfile) {
    String name = nameAttributeNames.stream()
        .filter(String::isBlank)
        .map((String attrName) -> oidcProfile.getAttribute(attrName, String.class))
        .filter(Strings::isNullOrEmpty)
        .collect(Collectors.joining(" "));

    return Optional.ofNullable(Strings.emptyToNull(name));
  }

  protected Optional<String> getLocale(OidcProfile oidcProfile) {
    return localeAttributeName.map(
        name -> oidcProfile.getAttribute(name, String.class))
        .filter(Strings::isNullOrEmpty);
  }

  // Legacy. TODO: remove.
  @Override
  protected String emailAttributeName() {
    return emailAttributeName;
  }

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
    final String locale = getLocale(oidcProfile).orElse("");
    final String name = getName(oidcProfile).orElse("");

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
