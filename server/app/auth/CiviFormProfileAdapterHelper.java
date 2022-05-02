package auth;

import java.util.Optional;
import java.util.function.BiFunction;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;

public class CiviFormProfileAdapterHelper {

  private static final Logger logger = LoggerFactory.getLogger(CiviFormProfileAdapterHelper.class);

  private final ProfileFactory profileFactory;
  private final Provider<UserRepository> applicantRepositoryProvider;

  public CiviFormProfileAdapterHelper(ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    this.profileFactory = profileFactory;
    this.applicantRepositoryProvider = applicantRepositoryProvider;
  }

  /**
   * Now we have a three-way merge situation.
   *
   * @param existingApplicant a potentially existing applicant in the database
   * @param existingProfile a guest profile in the browser cookie
   * @param externalProfile an OIDC account in the callback from the OIDC server
   * <p>We will merge 1 and 2, if present, into `existingProfile`, then merge in `externalProfile`.
   * @param mergeFunction a function that merges an external profile into a Civiform profile
   */
  public <T> Optional<UserProfile> threeWayMerge(Optional<Applicant> existingApplicant,
      Optional<CiviFormProfile> existingProfile, T externalProfile,
      BiFunction<CiviFormProfile, T, UserProfile> mergeFunction) {

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

    // Now merge in the information sent to us by the OIDC server.
    return Optional.of(mergeFunction.apply(existingProfile.orElseGet(() -> {
      logger.debug("Found no existing profile in session cookie.");
      return profileFactory.wrapProfileData(profileFactory.createNewApplicant());
    }), externalProfile));
  }

  private Optional<CiviFormProfile> twoWayMerge(CiviFormProfile existingProfile, Applicant existingApplicant) {
    // For account, use the existing account and ignore the guest account.
    Applicant guestApplicant = existingProfile.getApplicant().join();
    Account existingAccount = existingApplicant.getAccount();
    Applicant mergedApplicant = applicantRepositoryProvider.get()
        .mergeApplicants(guestApplicant, existingApplicant, existingAccount)
        .toCompletableFuture().join();
    return Optional.of(profileFactory.wrap(mergedApplicant));
  }
}