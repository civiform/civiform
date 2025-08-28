package auth;

import java.util.Optional;
import java.util.function.BiFunction;
import javax.inject.Provider;
import models.AccountModel;
import models.ApplicantModel;
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
   * Performs a three-way merge between an existing applicant in the database, a guest profile in
   * session storage, and an external profile from an external authentication provider
   *
   * @param applicantInDatabase a potentially existing applicant in the database
   * @param existingGuestProfile a guest profile from the browser session
   * @param authProviderProfile profile data from an external auth provider, such as OIDC
   * @param mergeFunction a function that merges an external profile into a Civiform profile, or
   *     provides one if it doesn't exist
   */
  public <T> Optional<UserProfile> mergeProfiles(
      Optional<ApplicantModel> applicantInDatabase,
      Optional<CiviFormProfile> existingGuestProfile,
      T authProviderProfile,
      BiFunction<Optional<CiviFormProfile>, T, UserProfile> mergeFunction) {
    // Merge the applicant, if it exists, with the guest profile.
    Optional<CiviFormProfile> applicantProfile =
        applicantInDatabase
            .map(
                applicantModel ->
                    mergeApplicantAndGuestProfile(applicantModel, existingGuestProfile))
            .or(() -> existingGuestProfile);

    // Merge authProviderProfile into the partially merged profile to finish.
    // Merge function will create a new CiviFormProfile if it doesn't exist,
    // or otherwise handle it
    return Optional.of(mergeFunction.apply(applicantProfile, authProviderProfile));
  }

  private CiviFormProfile mergeApplicantAndGuestProfile(
      ApplicantModel applicantInDatabase, Optional<CiviFormProfile> optionalGuestProfile) {
    final CiviFormProfile guestProfile;
    if (optionalGuestProfile.isEmpty()
        || optionalGuestProfile.get().getApplicant().join().getApplications().isEmpty()) {
      // Easy merge case - we have an existing applicant, but no guest profile (or a guest profile
      // with no applications). This will be the most common.
      guestProfile = profileFactory.wrap(applicantInDatabase);
    } else {
      // Merge the two applicants and prefer the newer one.
      guestProfile = mergeApplicantAndGuestProfile(applicantInDatabase, optionalGuestProfile.get());
    }
    // Ideally, the applicant id would already be populated in `guestProfile`. However, there
    // could be profiles in user sessions that were created before we started populating this
    // info.
    guestProfile.storeApplicantIdInProfile(applicantInDatabase.id);
    return guestProfile;
  }

  private CiviFormProfile mergeApplicantAndGuestProfile(
      ApplicantModel applicantInDatabase, CiviFormProfile sessionGuestProfile) {
    // Merge guest applicant data with already existing account in database.
    // TODO(#11304#issuecomment-3233634460): this merges the older account
    // into the newer which is likely incorrect.
    ApplicantModel guestApplicant = sessionGuestProfile.getApplicant().join();
    AccountModel existingAccount = applicantInDatabase.getAccount();
    ApplicantModel mergedApplicant =
        applicantRepositoryProvider
            .get()
            .mergeApplicantsOlderIntoNewer(guestApplicant, applicantInDatabase, existingAccount)
            .toCompletableFuture()
            .join();
    return profileFactory.wrap(mergedApplicant);
  }
}
