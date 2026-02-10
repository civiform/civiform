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
            () ->
                transactionManager.execute(
                    () -> {
                      // Merge the applicant with the guest profile.
                      Optional<CiviFormProfile> applicantProfile =
                          mergeApplicantAndGuestProfile(applicantInDatabase, existingGuestProfile);

                      // Merge authProviderProfile into the partially merged profile to finish.
                      // Merge function will create a new CiviFormProfile if it doesn't exist,
                      // or otherwise handle it
                      return Optional.of(
                          mergeFunction.apply(applicantProfile, authProviderProfile));
                    }),
            dbExecutionContext)
        .join();
  }

  private Optional<CiviFormProfile> mergeApplicantAndGuestProfile(
      Optional<ApplicantModel> optionalApplicantInDatabase,
      Optional<CiviFormProfile> optionalGuestProfile) {
    // This method makes some subjective decisions around the guest
    // profile's data which is informed by an applicant being present or not.

    boolean useApplicantModel = optionalApplicantInDatabase.isPresent();
    // If there's no applicant, retain whatever guest data there is, if any.
    // This represents the user logging in for the first time.
    if (!useApplicantModel) {
      return optionalGuestProfile;
    }

    // If there is an applicant, only retain guest information if it has
    // applications. We won't retain question answers, etc if there are none.
    // This represents a guest logging in to an existing civiform user, and
    // we only want to retain their data if they took an affirmative action
    // as the guest to submit data.
    boolean useGuestProfile =
        optionalGuestProfile.isPresent()
            && !optionalGuestProfile.get().getApplicant().join().getApplications().isEmpty();

    final CiviFormProfile profile;
    if (!useGuestProfile) {
      // Easy merge case - we have an existing applicant, but no guest profile (or a guest profile
      // with no applications). This will be the most common.
      profile = profileFactory.wrap(optionalApplicantInDatabase.get());
    } else {
      // Merge the two applicants.
      profile =
          mergeApplicantAndGuestProfile(
              optionalApplicantInDatabase.get(), optionalGuestProfile.get());
    }
    // Ideally, the applicant id would already be populated in `profile`.
    // However, there could be profiles in user sessions that were created
    // before we started populating this info.
    profile.storeApplicantIdInProfile(optionalApplicantInDatabase.get().id);
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
