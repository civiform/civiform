package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import durablejobs.DurableJob;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import models.ApplicantModel;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AccountRepository;
import services.WellKnownPaths;
import services.applicant.ApplicantData;
import services.settings.SettingsService;

/**
 * In this job, we always copy data from the Well Known Path (WKP) to the Primary Applicant Info
 * (PAI) columns, if the WKP data is present. Data in the PAI columns will get overwritten. This is
 * so that if a preseeded question is answered while the feature flag is off (or in the case of DOB,
 * a question whose name happens to match where we are putting the data from TI client creation),
 * that data gets synced over to the PAI columns. The exception is for email address, for which
 * there is no WKP. We will copy the account email over to the PAI column only if the PAI column is
 * empty.
 *
 * <p>When updating a TI client, both PAI and WKP get updated with the new data, so the two
 * locations should remain in sync.
 *
 * <p>When the flag is on, this job will be a no-op, as we will be removing WKP and PAI should be
 * the authoritative source of truth for the data about the applicant. Because the flag will be
 * ADMIN_WRITEABLE at first, then ADMIN_READABLE when we want to turn it on for everybody, and we
 * don't have access to a request object, we need to check both the database and the config file
 * setting to determine if the flag is on.
 */
public final class MigratePrimaryApplicantInfoJob extends DurableJob {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MigratePrimaryApplicantInfoJob.class);

  private final PersistedDurableJobModel persistedDurableJob;
  private final AccountRepository accountRepository;
  private final SettingsService settingsService;
  private final Config config;

  public MigratePrimaryApplicantInfoJob(
      PersistedDurableJobModel persistedDurableJob,
      AccountRepository accountRepository,
      SettingsService settingsService,
      Config config) {
    this.persistedDurableJob = checkNotNull(persistedDurableJob);
    this.accountRepository = checkNotNull(accountRepository);
    this.settingsService = checkNotNull(settingsService);
    this.config = checkNotNull(config);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    boolean doMigration;
    Optional<ImmutableMap<String, String>> writeableSettings =
        settingsService.loadSettings().toCompletableFuture().join();
    if (writeableSettings.isPresent()
        && writeableSettings.get().containsKey("PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED")) {
      doMigration =
          !writeableSettings.get().get("PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED").equals("true");
    } else {
      try {
        doMigration = !config.getString("primary_applicant_info_questions_enabled").equals("true");
      } catch (ConfigException.Missing e) {
        doMigration = false;
      }
    }
    if (!doMigration) {
      LOGGER.info(
          "PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED feature flag is set. Will not migrate data from"
              + " Well Known Paths to Primary Applicant Info columns.");
      return;
    }

    List<ApplicantModel> applicants =
        accountRepository.findApplicantsNeedingPrimaryApplicantInfoDataMigration().findList();

    for (ApplicantModel applicant : applicants) {
      ApplicantData applicantData = applicant.getApplicantData();
      Optional<String> firstName = applicantData.readString(WellKnownPaths.APPLICANT_FIRST_NAME);
      Optional<String> middleName = applicantData.readString(WellKnownPaths.APPLICANT_MIDDLE_NAME);
      Optional<String> lastName = applicantData.readString(WellKnownPaths.APPLICANT_LAST_NAME);
      Optional<String> nameSuffix =
          applicantData.readAsString(WellKnownPaths.APPLICANT_NAME_SUFFIX);
      String emailAddress = applicant.getAccount().getEmailAddress();
      // Note that this will only return a value if it's set via TI client
      // creation/edit. This is because that code sets a string here directly,
      // whereas a phone number question would have an object here that includes
      // the number and country code. We only really care about porting over
      // TI client data for this field.
      Optional<String> phoneNumber =
          applicantData.readString(WellKnownPaths.APPLICANT_PHONE_NUMBER);
      Optional<LocalDate> dob =
          applicantData
              .readDate(WellKnownPaths.APPLICANT_DOB)
              .or(() -> applicantData.readDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED));

      firstName.ifPresent(first -> applicant.setFirstName(first));
      middleName.ifPresent(middle -> applicant.setMiddleName(middle));
      lastName.ifPresent(last -> applicant.setLastName(last));
      nameSuffix.ifPresent(suffix -> applicant.setSuffix(suffix));
      phoneNumber.ifPresent(phone -> applicant.setPhoneNumber(phone));
      dob.ifPresent(date -> applicant.setDateOfBirth(date));
      if (!Strings.isNullOrEmpty(emailAddress) && applicant.getEmailAddress().isEmpty()) {
        applicant.setEmailAddress(emailAddress);
      }
      applicant.save();
    }
    LOGGER.info("Migrated primary applicant info for {} applicants", applicants.size());
  }
}
