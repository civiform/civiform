package auth;

import java.util.Optional;
import java.util.function.BiFunction;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
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
   * @param guestProfile a guest profile from the browser session
   * @param authProviderProfile profile data from an external auth provider, such as OIDC
   * @param mergeFunction a function that merges an external profile into a Civiform profile, or
   *     provides one if it doesn't exist
   */
  public <T> Optional<UserProfile> mergeProfiles(
      Optional<Applicant> applicantInDatabase,
      Optional<CiviFormProfile> guestProfile,
      T authProviderProfile,
      BiFunction<Optional<CiviFormProfile>, T, UserProfile> mergeFunction) {

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
      // Store applicant ID in profile so that:
      // - we don't need it in the URL for applicant actions
      // - we don't need to look it up based on the account corresponding to the profile
      guestProfile
          .orElseThrow()
          .getProfileData()
          .addAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, applicantInDatabase.get().id);
    }

    // Merge externalProfile into existingProfile.
    // Merge function will create a new CiviFormProfile if it doesn't exist,
    // or otherwise handle it
    return Optional.of(mergeFunction.apply(guestProfile, authProviderProfile));
  }

  private CiviFormProfile mergeProfiles(
      Applicant applicantInDatabase, CiviFormProfile sessionGuestProfile) {
    // Merge guest applicant data into already existing account in database
    Applicant guestApplicant = sessionGuestProfile.getApplicant().join();
    Account existingAccount = applicantInDatabase.getAccount();
    Applicant mergedApplicant =
        applicantRepositoryProvider
            .get()
            .mergeApplicants(guestApplicant, applicantInDatabase, existingAccount)
            .toCompletableFuture()
            .join();
    return profileFactory.wrap(mergedApplicant);
  }
}
