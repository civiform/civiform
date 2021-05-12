package auth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import javax.inject.Provider;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
import repository.UserRepository;

/**
 * This class takes an existing UAT profile and augments it with the information from an IDCS
 * profile.
 */
public class IdcsProfileAdapter extends UatProfileAdapter {

  public IdcsProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
  }

  @Override
  protected String emailAttributeName() {
    return "user_emailid";
  }

  @Override
  protected ImmutableSet<Roles> roles(UatProfile profile, OidcProfile oidcProfile) {
    if (profile.getAccount().join().getMemberOfGroup().isPresent()) {
      return ImmutableSet.of(Roles.ROLE_APPLICANT, Roles.ROLE_TI);
    }
    return ImmutableSet.of(Roles.ROLE_APPLICANT);
  }

  @Override
  public UatProfileData mergeUatProfile(UatProfile uatProfile, OidcProfile oidcProfile) {
    String locale = oidcProfile.getAttribute("user_locale", String.class);
    ImmutableList.Builder<CompletionStage<Void>> dbOperations =
        new ImmutableList.Builder<CompletionStage<Void>>();
    if (locale != null && !locale.isEmpty()) {
      dbOperations.add(
          uatProfile
              .getApplicant()
              .thenApplyAsync(
                  applicant -> {
                    applicant.getApplicantData().setPreferredLocale(Locale.forLanguageTag(locale));
                    applicant.save();
                    return null;
                  }));
    }
    String displayName = oidcProfile.getAttribute("user_displayname", String.class);
    if (displayName != null && !displayName.isEmpty()) {
      dbOperations.add(
          uatProfile
              .getApplicant()
              .thenApplyAsync(
                  applicant -> {
                    applicant.getApplicantData().setUserName(displayName);
                    applicant.save();
                    return null;
                  }));
    }
    for (CompletionStage<Void> dbOp : dbOperations.build()) {
      dbOp.toCompletableFuture().join();
    }

    return super.mergeUatProfile(uatProfile, oidcProfile);
  }

  @Override
  public UatProfileData uatProfileFromOidcProfile(OidcProfile profile) {
    return mergeUatProfile(
        profileFactory.wrapProfileData(profileFactory.createNewApplicant()), profile);
  }

  @Override
  protected void possiblyModifyConfigBasedOnCred(Credentials cred) {
    // The flow here is not immediately intuitive.  IDCS is to blame.  :)
    // The normal flow for authenticating a user is "get user's data via POST.
    // Decode it, check that it is signed, and use it." IDCS throws in an extra step
    // here - in order to get IDCS's signing key, we need to provide an Authorization
    // header proving that we have a good reason to use the signing key.
    // Pac4j and associated tools are not well-suited to that, because it's
    // a deviation from the OIDC spec.  Pac4j has the concept of a "resource retriever",
    // which is used to fetch things like the signing key.  They are meant to
    // be configured once and used indefinitely, but we only get the access
    // token at the time the user logs in and is redirected to our server.  So,
    // we need to slighly abuse the notion of a resource retriever.  We create our
    // own modified resource retriever which has access to the required token.

    // Note that there would normally be a significant thread-safety issue here - but
    // we actually don't need to match up signing key tokens with actual tokens, because
    // any valid token is sufficient.
    this.configuration.setResourceRetriever(
        new DefaultResourceRetriever(
            this.configuration.getConnectTimeout(), this.configuration.getReadTimeout()) {
          @Override
          public Map<String, List<String>> getHeaders() {
            Map<String, List<String>> headers = super.getHeaders();
            if (((OidcCredentials) cred).getAccessToken() == null) {
              return headers;
            }
            if (headers == null) {
              headers = new HashMap<>();
            }
            String authHeader = ((OidcCredentials) cred).getAccessToken().toAuthorizationHeader();
            headers.put("Authorization", List.of(authHeader));
            return headers;
          }
        });
  }
}
