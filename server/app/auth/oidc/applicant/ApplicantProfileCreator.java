package auth.oidc.applicant;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.IdentityProviderType;
import auth.Role;
import auth.oidc.CiviformOidcProfileCreator;
import auth.oidc.OidcClientProviderParams;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.pac4j.core.context.WebContext;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;

/**
 * This class ensures that the OidcProfileCreator that both the AD and IDCS clients use will
 * generate a CiviFormProfile object. This is necessary for merging those accounts with existing
 * accounts - that's not usually needed in web applications which is why we have to write this class
 * - pac4j doesn't come with it. It's abstract because AD and IDCS need slightly different
 * implementations of the two abstract methods.
 */
public abstract class ApplicantProfileCreator extends CiviformOidcProfileCreator {

  @VisibleForTesting public final String emailAttributeName;

  @VisibleForTesting public final Optional<String> localeAttributeName;

  @VisibleForTesting public final ImmutableList<String> nameAttributeNames;

  public ApplicantProfileCreator(
      OidcConfiguration configuration,
      OidcClient client,
      OidcClientProviderParams params,
      String emailAttributeName,
      @Nullable String localeAttributeName,
      ImmutableList<String> nameAttributeNames) {
    super(configuration, client, params);
    this.emailAttributeName = Preconditions.checkNotNull(emailAttributeName);
    this.localeAttributeName = Optional.ofNullable(localeAttributeName);
    this.nameAttributeNames = Preconditions.checkNotNull(nameAttributeNames);
  }

  private Optional<String> getName(OidcProfile oidcProfile) {
    String name =
        nameAttributeNames.stream()
            .filter(s -> !s.isBlank())
            .map((String attrName) -> oidcProfile.getAttribute(attrName, String.class))
            .filter(s -> !Strings.isNullOrEmpty(s))
            .collect(Collectors.joining(" "));

    // Special case for https://github.com/civiform/civiform/issues/8344, Auth0 (and possibly other
    // auth providers) will put in the user's email for the "name" attribute, if no name is
    // specified.
    //
    // Since we actually care about whether a real name was provided or not, return empty
    // if it matches the email.
    if (name.equals(oidcProfile.getAttribute(emailAttributeName()))) {
      return Optional.empty();
    }

    return Optional.ofNullable(Strings.emptyToNull(name));
  }

  private Optional<String> getLocale(OidcProfile oidcProfile) {
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
    // System.out.println("ssandbekkhaug createEmptyCiviFormProfile");
    return profileFactory.wrapProfileData(profileFactory.createNewApplicant());
  }

  @Override
  protected final ImmutableSet<Role> roles(CiviFormProfile profile, OidcProfile oidcProfile) {
    if (isTrustedIntermediary(profile)) {
      // Give ROLE_APPLICANT in addition to ROLE_TI so that the TI can perform applicant actions.
      return ImmutableSet.of(Role.ROLE_APPLICANT, Role.ROLE_TI);
    }
    return ImmutableSet.of(Role.ROLE_APPLICANT);
  }

  @Override
  protected final void adaptForRole(CiviFormProfile profile, ImmutableSet<Role> roles) {
    // Not used for applicants.
  }

  /** Merge the two provided profiles into a new CiviFormProfileData. */
  @Override
  protected final CiviFormProfileData mergeCiviFormProfile(
      CiviFormProfile civiformProfile, OidcProfile oidcProfile, WebContext context) {
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

    return super.mergeCiviFormProfile(civiformProfile, oidcProfile, context);
  }

  @Override
  protected IdentityProviderType identityProviderType() {
    return IdentityProviderType.APPLICANT_IDENTITY_PROVIDER;
  }
}
