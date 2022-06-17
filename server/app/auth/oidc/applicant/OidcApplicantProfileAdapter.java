package auth.oidc.applicant;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.Roles;
import auth.oidc.OidcProfileAdapter;
import com.beust.jcommander.internal.Nullable;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
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

  private final Optional<String> getName(OidcProfile oidcProfile) {
    String name =
        nameAttributeNames.stream()
            .filter(s -> !s.isBlank())
            .map((String attrName) -> oidcProfile.getAttribute(attrName, String.class))
            .filter(s -> !Strings.isNullOrEmpty(s))
            .collect(Collectors.joining(" "));

    return Optional.ofNullable(Strings.emptyToNull(name));
  }

  private final Optional<String> getLocale(OidcProfile oidcProfile) {
    return localeAttributeName
        .filter(s -> !s.isBlank())
        .map(name -> oidcProfile.getAttribute(name, String.class))
        .filter(s -> !Strings.isNullOrEmpty(s));
  }

  @Override
  protected final String emailAttributeName() {
    return emailAttributeName;
  }

  /** Create a totally new Applicant CiviForm profile informed by the provided OidcProfile. */
  @Override
  public final CiviFormProfile createEmptyCiviFormProfile(OidcProfile profile) {
    return profileFactory.wrapProfileData(profileFactory.createNewApplicant());
  }

  protected final boolean isTrustedIntermediary(CiviFormProfile profile) {
    return profile.getAccount().join().getMemberOfGroup().isPresent();
  }

  @Override
  protected final ImmutableSet<Roles> roles(CiviFormProfile profile, OidcProfile oidcProfile) {
    if (isTrustedIntermediary(profile)) {
      return ImmutableSet.of(Roles.ROLE_APPLICANT, Roles.ROLE_TI);
    }
    return ImmutableSet.of(Roles.ROLE_APPLICANT);
  }

  @Override
  protected final void adaptForRole(CiviFormProfile profile, ImmutableSet<Roles> roles) {
    // Not used for applicants.
  }

  @Override
  protected void possiblyModifyConfigBasedOnCred(Credentials cred) {
    // Only used for Idcs.
  }

  /** Merge the two provided profiles into a new CiviFormProfileData. */
  @Override
  protected final CiviFormProfileData mergeCiviFormProfile(
      CiviFormProfile civiformProfile, OidcProfile oidcProfile) {
    final Optional<String> maybeLocale = getLocale(oidcProfile);
    final Optional<String> maybeName = getName(oidcProfile);

    if (maybeLocale.isPresent() || maybeName.isPresent()) {
      civiformProfile
          .getApplicant()
          .thenApplyAsync(
              applicant -> {
                maybeLocale.ifPresent(
                    locale ->
                        applicant
                            .getApplicantData()
                            .setPreferredLocale(Locale.forLanguageTag(locale)));

                maybeName.ifPresent(name -> applicant.getApplicantData().setUserName(name));

                applicant.save();
                return null;
              })
          .toCompletableFuture()
          .join();
    }

    return super.mergeCiviFormProfile(civiformProfile, oidcProfile);
  }
}
