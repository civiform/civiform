package auth;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import javax.inject.Provider;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.ApplicantRepository;

/**
 * This class takes an existing UAT profile and augments it with the information from an IDCS
 * profile.
 */
public class IdcsProfileAdapter extends UatProfileAdapter {

  public IdcsProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<ApplicantRepository> applicantRepositoryProvider) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
  }

  @Override
  protected String emailAttributeName() {
    return "user_emailid";
  }

  @Override
  public UatProfileData mergeUatProfile(UatProfile uatProfile, OidcProfile oidcProfile) {
    String locale = oidcProfile.getAttribute("user_locale", String.class);
    List<CompletionStage<Void>> dbOperations = List.of();
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
    for (CompletionStage<Void> dbOp : dbOperations) {
      dbOp.toCompletableFuture().join();
    }

    return super.mergeUatProfile(uatProfile, oidcProfile);
  }

  @Override
  public UatProfileData uatProfileFromOidcProfile(OidcProfile profile) {
    return mergeUatProfile(
        profileFactory.wrapProfileData(profileFactory.createNewApplicant()), profile);
  }
}
