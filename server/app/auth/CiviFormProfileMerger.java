package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.Optional;
import java.util.function.BiFunction;
import javax.inject.Provider;
import models.AccountModel;
import models.ApplicantModel;
import org.pac4j.core.profile.UserProfile;
import repository.AccountRepository;
import repository.DatabaseExecutionContext;
import repository.TransactionManager;

/** Helper class for common {@code UserProfile} merging logic. */
public final class CiviFormProfileMerger {

  private final ProfileFactory profileFactory;
  private final Provider<AccountRepository> applicantRepositoryProvider;
  private final DatabaseExecutionContext dbExecutionContext;
  private final TransactionManager transactionManager;

  public CiviFormProfileMerger(
      ProfileFactory profileFactory,
      Provider<AccountRepository> applicantRepositoryProvider,
      DatabaseExecutionContext dbExecutionContext) {
    this.profileFactory = profileFactory;
    this.applicantRepositoryProvider = applicantRepositoryProvider;
    this.dbExecutionContext = dbExecutionContext;
    this.transactionManager = new TransactionManager();
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
    return supplyAsync(
            () -> {
              return transactionManager.execute(
                  () -> {
                    // Merge the applicant with the guest profile.
                    Optional<CiviFormProfile> applicantProfile =
                        mergeApplicantAndGuestProfile(applicantInDatabase, existingGuestProfile);

                    // Merge authProviderProfile into the partially merged profile to finish.
                    // Merge function will create a new CiviFormProfile if it doesn't exist,
                    // or otherwise handle it
                    return Optional.of(mergeFunction.apply(applicantProfile, authProviderProfile));
                  });
            },
            dbExecutionContext)
        .join();
  }

  private Optional<CiviFormProfile> mergeApplicantAndGuestProfile(
      Optional<ApplicantModel> optionalApplicantInDatabase,
      Optional<CiviFormProfile> optionalGuestProfile) {
    boolean useApplicantModel = optionalApplicantInDatabase.isPresent();
    boolean useGuestProfile =
        optionalGuestProfile.isPresent()
            && !optionalGuestProfile.get().getApplicant().join().getApplications().isEmpty();
    if (!useApplicantModel && !useGuestProfile) {
      return Optional.empty();
    }

    final CiviFormProfile profile;
    if (useApplicantModel && !useGuestProfile) {
      // Easy merge case - we have an existing applicant, but no guest profile (or a guest profile
      // with no applications). This will be the most common.
      profile = profileFactory.wrap(optionalApplicantInDatabase.get());
    } else if (useGuestProfile && !useApplicantModel) {
      profile = optionalGuestProfile.get();
    } else {
      // Merge the two applicants and prefer the newer one.
      profile =
          mergeApplicantAndGuestProfile(
              optionalApplicantInDatabase.get(), optionalGuestProfile.get());
    }
    // Ideally, the applicant id would already be populated in `profile`.
    // However, there could be profiles in user sessions that were created
    // before we started populating this info.
    optionalApplicantInDatabase.ifPresent(
        applicantModel -> profile.storeApplicantIdInProfile(applicantModel.id));
    return Optional.of(profile);
  }

  private CiviFormProfile mergeApplicantAndGuestProfile(
      ApplicantModel applicantInDatabase, CiviFormProfile sessionGuestProfile) {
    // Merge guest applicant data with already existing account in database.
    // TODO(#11304#issuecomment-3233634460): this merges the older account
    // into the newer which is incorrect.
    ApplicantModel guestApplicant = sessionGuestProfile.getApplicant().join();
    AccountModel existingAccount = applicantInDatabase.getAccount();
    ApplicantModel mergedApplicant =
        applicantRepositoryProvider
            .get()
            .mergeApplicantsOlderIntoNewer(guestApplicant, applicantInDatabase, existingAccount);
    return profileFactory.wrap(mergedApplicant);
  }
}
