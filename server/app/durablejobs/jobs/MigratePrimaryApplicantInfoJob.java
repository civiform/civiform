package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import durablejobs.DurableJob;
import java.time.LocalDate;
import java.util.Optional;
import models.PersistedDurableJobModel;
import repository.AccountRepository;
import services.WellKnownPaths;
import services.applicant.ApplicantData;

public final class MigratePrimaryApplicantInfoJob extends DurableJob {

  private final PersistedDurableJobModel persistedDurableJob;
  private final AccountRepository accountRepository;

  public MigratePrimaryApplicantInfoJob(
      PersistedDurableJobModel persistedDurableJob, AccountRepository accountRepository) {
    this.persistedDurableJob = checkNotNull(persistedDurableJob);
    this.accountRepository = checkNotNull(accountRepository);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    accountRepository
        .findApplicantsNeedingPrimaryApplicantInfoDataMigration()
        .findEach(
            (applicant) -> {
              ApplicantData applicantData = applicant.getApplicantData();
              Optional<String> firstName =
                  applicantData.readString(WellKnownPaths.APPLICANT_FIRST_NAME);
              Optional<String> middleName =
                  applicantData.readString(WellKnownPaths.APPLICANT_MIDDLE_NAME);
              Optional<String> lastName =
                  applicantData.readString(WellKnownPaths.APPLICANT_LAST_NAME);
              String emailAddress = applicant.getAccount().getEmailAddress();
              Optional<String> phoneNumber =
                  applicantData.readString(WellKnownPaths.APPLICANT_PHONE_NUMBER);
              Optional<LocalDate> dob =
                  applicantData
                      .readDate(WellKnownPaths.APPLICANT_DOB)
                      .or(() -> applicantData.readDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED));

              Optional<String> paiFirstName = applicant.getFirstName();
              boolean firstNameNeedsUpdating =
                  paiFirstName.isEmpty() || paiFirstName.get().contains("@");
              if (firstName.isPresent() && firstNameNeedsUpdating) {
                applicant.setFirstName(firstName.get());
              }
              if (middleName.isPresent() && applicant.getMiddleName().isEmpty()) {
                applicant.setMiddleName(middleName.get());
              }
              if (lastName.isPresent() && applicant.getLastName().isEmpty()) {
                applicant.setLastName(lastName.get());
              }
              if (!Strings.isNullOrEmpty(emailAddress) && applicant.getEmailAddress().isEmpty()) {
                applicant.setEmailAddress(emailAddress);
              }
              if (phoneNumber.isPresent() && applicant.getPhoneNumber().isEmpty()) {
                applicant.setPhoneNumber(phoneNumber.get());
              }
              if (dob.isPresent() && applicant.getDateOfBirth().isEmpty()) {
                applicant.setDateOfBirth(dob.get());
              }
              applicant.save();
            });
  }
}
