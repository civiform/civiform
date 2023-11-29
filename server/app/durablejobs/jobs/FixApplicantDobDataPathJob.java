package durablejobs.jobs;

import com.google.common.base.Preconditions;
import durablejobs.DurableJob;
import java.time.LocalDate;
import models.PersistedDurableJobModel;
import repository.AccountRepository;
import services.WellKnownPaths;
import services.applicant.ApplicantData;

/**
 * Fixes applicant data objects that have the wrong path for applicant_date_of_birth and are causing
 * errors when TIs attempt to apply on behalf of clients. This job finds all applicant data objects
 * with applicant_date_of_birth set to a number and changes them to nested objects of the form
 * {applicant_date_of_birth: {date: number}}.
 */
public final class FixApplicantDobDataPathJob extends DurableJob {

  private final PersistedDurableJobModel persistedDurableJob;
  private final AccountRepository accountRepository;

  public FixApplicantDobDataPathJob(
      AccountRepository accountRepository, PersistedDurableJobModel persistedDurableJob) {
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    accountRepository
        .findApplicantsWithIncorrectDobPath()
        .findEach(
            (applicant) -> {
              ApplicantData applicantData = applicant.getApplicantData();
              LocalDate dob = applicantData.getDeprecatedDateOfBirth().get();
              // Clear the old path off the json data before inserting the new one
              applicantData
                  .getDocumentContext()
                  .delete(WellKnownPaths.APPLICANT_DOB_DEPRECATED.toString());
              applicantData.setDateOfBirth(dob.toString());
              applicant.save();
            });
  }
}
