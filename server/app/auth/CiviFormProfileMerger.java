package auth;

import java.util.Optional;
import java.util.function.BiFunction;
import javax.inject.Provider;
import models.AccountModel;
import models.Applicant;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.UserProfile;
import repository.AccountRepository;

/** Helper class for common {@code UserProfile} merging logic. */
public final class CiviFormProfileMerger {

  private final ProfileFactory profileFactory;
  private final Provider<AccountRepository> applicantRepositoryProvider;

  public CiviFormProfileMerger(
      ProfileFactory profileFactory, Provider<AccountRepository> applicantRepositoryProvider) {
    this.profileFactory = profileFactory;
    this.applicantRepositoryProvider = applicantRepositoryProvider;
  }

  /**
   * Performs a three way merge between an existing applicant in the database, a guest profile in
   * session storage, and an external profile from an external authentication provider
   *
   * @param applicantInDatabase a potentially existing applicant in the database
   * @param existingProfile a guest profile from the browser session
   * @param authProviderProfile profile data from an external auth provider, such as OIDC
   * @param mergeFunction a function that merges an external profile into a Civiform profile, or
   *     provides one if it doesn't exist
   */
  public <T> Optional<UserProfile> mergeProfiles(
      Optional<Applicant> applicantInDatabase,
      Optional<CiviFormProfile> existingProfile,
      T authProviderProfile,
      BiFunction<Optional<CiviFormProfile>, T, UserProfile> mergeFunction) {
    existingProfile = mergeApplicantAndGuestProfile(applicantInDatabase, existingProfile);

    // Merge externalProfile into existingProfile.
    // Merge function will create a new CiviFormProfile if it doesn't exist,
    // or otherwise handle it
    return Optional.of(mergeFunction.apply(existingProfile, authProviderProfile));
  }

  /**
   * Performs a three way merge between an existing applicant in the database, a guest profile in
   * session storage, and an external profile from an external authentication provider
   *
   * @param applicantInDatabase a potentially existing applicant in the database
   * @param existingProfile a guest profile from the browser session
   * @param authProviderProfile profile data from an external auth provider, such as OIDC
   * @param context WebContext of current request
   * @param mergeFunction a function that merges an external profile into a Civiform profile, or
   *     provides one if it doesn't exist
   */
  public <T> Optional<UserProfile> mergeProfiles(
      Optional<Applicant> applicantInDatabase,
      Optional<CiviFormProfile> existingProfile,
      T authProviderProfile,
      WebContext context,
      TriFunction<Optional<CiviFormProfile>, T, WebContext, UserProfile> mergeFunction) {
    existingProfile = mergeApplicantAndGuestProfile(applicantInDatabase, existingProfile);

    // Merge externalProfile into existingProfile.
    // Merge function will create a new CiviFormProfile if it doesn't exist,
    // or otherwise handle it
    return Optional.of(mergeFunction.apply(existingProfile, authProviderProfile, context));
  }

  private Optional<CiviFormProfile> mergeApplicantAndGuestProfile(
      Optional<Applicant> applicantInDatabase, Optional<CiviFormProfile> guestProfile) {
    if (applicantInDatabase.isPresent()) {
      if (guestProfile.isEmpty()
          || guestProfile.get().getApplicant().join().getApplications().isEmpty()) {
        // Easy merge case - we have an existing applicant, but no guest profile (or a guest profile
        // with no applications). This will be the most common.
        guestProfile = Optional.of(profileFactory.wrap(applicantInDatabase.get()));
      } else {
        // Merge the two applicants and prefer the newer one.
        guestProfile = Optional.of(mergeProfiles(applicantInDatabase.get(), guestProfile.get()));
      }
      // Ideally, the applicant id would already be populated in `guestProfile`. However, there
      // could be profiles in user sessions that were created before we started populating this
      // info.
      storeApplicantIdInProfile(guestProfile.orElseThrow(), applicantInDatabase.orElseThrow());
    }
    return guestProfile;
  }

  private void storeApplicantIdInProfile(CiviFormProfile profile, Applicant applicant) {
    if (!profile.getProfileData().containsAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME)) {
      profile
          .getProfileData()
          .addAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, applicant.id);
    }
  }

  private CiviFormProfile mergeProfiles(
      Applicant applicantInDatabase, CiviFormProfile sessionGuestProfile) {
    // Merge guest applicant data into already existing account in database
    Applicant guestApplicant = sessionGuestProfile.getApplicant().join();
    AccountModel existingAccount = applicantInDatabase.getAccount();
    Applicant mergedApplicant =
        applicantRepositoryProvider
            .get()
            .mergeApplicants(guestApplicant, applicantInDatabase, existingAccount)
            .toCompletableFuture()
            .join();
    return profileFactory.wrap(mergedApplicant);
  }
}
