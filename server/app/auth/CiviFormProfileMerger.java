package auth;

import java.util.Optional;
import java.util.function.BiFunction;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.UserProfile;
import repository.UserRepository;

/** This class serves to abstract out common user profile merging logic into a shared helper. */
public class CiviFormProfileMerger {

  private final ProfileFactory profileFactory;
  private final Provider<UserRepository> applicantRepositoryProvider;

  public CiviFormProfileMerger(
      ProfileFactory profileFactory, Provider<UserRepository> applicantRepositoryProvider) {
    this.profileFactory = profileFactory;
    this.applicantRepositoryProvider = applicantRepositoryProvider;
  }

  /**
   * This method handles a three way merge between an existing applicant, an existing guest profile,
   * and an external profile
   *
   * @param existingApplicant a potentially existing applicant in the database
   * @param existingProfile a guest profile in the browser cookie
   * @param externalProfile profile data from an external auth provider, such as OIDC
   * @param mergeFunction a function that merges an external profile into a Civiform profile, or
   *     provides one if it doesn't exist
   */
  public <T> Optional<UserProfile> threeWayMerge(
      Optional<Applicant> existingApplicant,
      Optional<CiviFormProfile> existingProfile,
      T externalProfile,
      BiFunction<Optional<CiviFormProfile>, T, UserProfile> mergeFunction) {

    if (existingApplicant.isPresent()) {
      if (existingProfile.isEmpty()) {
        // Easy merge case - we have an existing applicant, but no guest profile.
        // This will be the most common.
        existingProfile = Optional.of(profileFactory.wrap(existingApplicant.get()));
      } else {
        // Merge the two applicants and prefer the newer one.
        existingProfile = twoWayMerge(existingProfile.get(), existingApplicant.get());
      }
    }

    // Merge externalProfile into existingProfile
    // Merge function will create a new CiviFormProfile if it doesn't exist,
    // or otherwise handle it in some way
    return Optional.of(mergeFunction.apply(existingProfile, externalProfile));
  }

  private Optional<CiviFormProfile> twoWayMerge(
      CiviFormProfile existingProfile, Applicant existingApplicant) {
    // For account, use the existing account and ignore the guest account.
    Applicant guestApplicant = existingProfile.getApplicant().join();
    Account existingAccount = existingApplicant.getAccount();
    Applicant mergedApplicant =
        applicantRepositoryProvider
            .get()
            .mergeApplicants(guestApplicant, existingApplicant, existingAccount)
            .toCompletableFuture()
            .join();
    return Optional.of(profileFactory.wrap(mergedApplicant));
  }
}
