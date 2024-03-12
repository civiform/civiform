package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import durablejobs.DurableJobName;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.annotation.TxIsolation;
import java.time.Instant;
import models.AccountModel;
import models.ApplicantModel;
import models.PersistedDurableJobModel;
import org.junit.Test;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.WellKnownPaths;
import services.applicant.ApplicantData;
import services.settings.SettingsService;

public class MigratePrimaryApplicantInfoJobTest extends ResetPostgres {

  private final Database database = DB.getDefault();

  private ApplicantModel createApplicant() {
    database.beginTransaction();
    AccountModel account = new AccountModel();
    account.save();
    ApplicantModel applicant = new ApplicantModel();
    applicant.setAccount(account);
    applicant.save();
    database.commitTransaction();
    return applicant;
  }

  private ApplicantModel createApplicantWithWellKnownPathData(boolean withPaiData) {
    ApplicantModel applicant = createApplicant();
    database.beginTransaction();
    AccountModel account = applicant.getAccount();
    account.setEmailAddress("account@email.com");
    account.save();
    ApplicantData applicantData = applicant.getApplicantData();
    applicantData.putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    applicantData.putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, "Luc");
    applicantData.putString(WellKnownPaths.APPLICANT_LAST_NAME, "Picard");
    applicantData.putDate(WellKnownPaths.APPLICANT_DOB, "2305-07-13");
    applicantData.putString(WellKnownPaths.APPLICANT_PHONE_NUMBER, "5038234000");
    if (withPaiData) {
      applicant.setFirstName("Kathryn");
      applicant.setMiddleName("");
      applicant.setLastName("Janeway");
      applicant.setDateOfBirth("2328-05-20");
      applicant.setPhoneNumber("2066842489");
      applicant.setEmailAddress("applicant@email.com");
    }
    applicant.save();
    database.commitTransaction();
    return applicant;
  }

  private void runJob() {
    runJob(/* paiFlagEnabled= */ false);
  }

  private void runJob(Boolean paiFlagEnabled) {
    database.beginTransaction(TxIsolation.SERIALIZABLE);

    SettingsService settingsService = instanceOf(SettingsService.class);
    ImmutableMap<String, String> settings =
        settingsService.loadSettings().toCompletableFuture().join().get();
    ImmutableMap.Builder<String, String> newSettings = ImmutableMap.builder();
    for (var entry : settings.entrySet()) {
      if (!entry.getKey().equals("PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED")) {
        newSettings.put(entry);
      }
    }
    newSettings.put("PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED", paiFlagEnabled.toString());
    settingsService.updateSettings(newSettings.build(), "test");
    database.commitTransaction();
    database.beginTransaction(TxIsolation.SERIALIZABLE);

    PersistedDurableJobModel job =
        new PersistedDurableJobModel(
            DurableJobName.MIGRATE_PRIMARY_APPLICANT_INFO.toString(), Instant.ofEpochMilli(0));
    MigratePrimaryApplicantInfoJob migrateJob =
        new MigratePrimaryApplicantInfoJob(
            job, instanceOf(AccountRepository.class), settingsService, instanceOf(Config.class));
    migrateJob.run();
    database.commitTransaction();
  }

  @Test
  public void run_doesNotMigrateDataWhenFlagIsOn() {
    ApplicantModel applicant = createApplicantWithWellKnownPathData(/* withPaiData= */ true);
    runJob(/* paiFlagEnabled= */ true);
    applicant.refresh();
    assertThat(applicant.getFirstName().get()).isEqualTo("Kathryn");
    assertThat(applicant.getMiddleName()).isEmpty();
    assertThat(applicant.getLastName().get()).isEqualTo("Janeway");
    assertThat(applicant.getDateOfBirth().get()).isEqualTo("2328-05-20");
    assertThat(applicant.getPhoneNumber().get()).isEqualTo("2066842489");
  }

  @Test
  public void run_migratesWhenPaiDataIsEmpty() {
    ApplicantModel applicant = createApplicantWithWellKnownPathData(/* withPaiData= */ false);
    runJob();
    applicant.refresh();
    assertThat(applicant.getFirstName().get()).isEqualTo("Jean");
    assertThat(applicant.getMiddleName().get()).isEqualTo("Luc");
    assertThat(applicant.getLastName().get()).isEqualTo("Picard");
    assertThat(applicant.getDateOfBirth().get()).isEqualTo("2305-07-13");
    assertThat(applicant.getPhoneNumber().get()).isEqualTo("5038234000");
    assertThat(applicant.getEmailAddress().get()).isEqualTo("account@email.com");
  }

  @Test
  public void run_migratesWhenPaiDataIsAlreadyPopulated() {
    ApplicantModel applicant = createApplicantWithWellKnownPathData(/* withPaiData= */ true);
    runJob();
    applicant.refresh();
    assertThat(applicant.getFirstName().get()).isEqualTo("Jean");
    assertThat(applicant.getMiddleName().get()).isEqualTo("Luc");
    assertThat(applicant.getLastName().get()).isEqualTo("Picard");
    assertThat(applicant.getDateOfBirth().get()).isEqualTo("2305-07-13");
    assertThat(applicant.getPhoneNumber().get()).isEqualTo("5038234000");
    assertThat(applicant.getEmailAddress().get()).isEqualTo("applicant@email.com");
  }

  @Test
  public void run_migratesWhenOnlySomeWkpDataIsPopulated() {
    ApplicantModel applicantFirstName = createApplicant();
    ApplicantModel applicantTiClient = createApplicant();
    ApplicantModel applicantEmailOnly = createApplicant();
    ApplicantModel applicantEmailPresent = createApplicant();

    database.beginTransaction();
    applicantFirstName.getApplicantData().putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    applicantFirstName.save();

    ApplicantData data = applicantTiClient.getApplicantData();
    data.putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    data.putString(WellKnownPaths.APPLICANT_LAST_NAME, "Picard");
    data.putDate(WellKnownPaths.APPLICANT_DOB, "2305-07-13");
    data.putString(WellKnownPaths.APPLICANT_PHONE_NUMBER, "5038234000");
    applicantTiClient.save();

    AccountModel account = applicantEmailOnly.getAccount();
    account.setEmailAddress("picard_account_1@starfleet.com");
    account.save();

    account = applicantEmailPresent.getAccount();
    account.setEmailAddress("picard_account_2@starfleet.com");
    account.save();
    applicantEmailPresent.setEmailAddress("picard_real_email@starfleet.com");
    applicantEmailPresent.save();
    database.commitTransaction();
    runJob();

    applicantFirstName.refresh();
    assertThat(applicantFirstName.getFirstName().get()).isEqualTo("Jean");
    assertThat(applicantFirstName.getMiddleName()).isEmpty();
    assertThat(applicantFirstName.getLastName()).isEmpty();
    assertThat(applicantFirstName.getDateOfBirth()).isEmpty();
    assertThat(applicantFirstName.getEmailAddress()).isEmpty();
    assertThat(applicantFirstName.getPhoneNumber()).isEmpty();
    assertThat(applicantFirstName.getCountryCode()).isEmpty();

    applicantTiClient.refresh();
    assertThat(applicantTiClient.getFirstName().get()).isEqualTo("Jean");
    assertThat(applicantTiClient.getMiddleName()).isEmpty();
    assertThat(applicantTiClient.getLastName().get()).isEqualTo("Picard");
    assertThat(applicantTiClient.getDateOfBirth().get()).isEqualTo("2305-07-13");
    assertThat(applicantTiClient.getPhoneNumber().get()).isEqualTo("5038234000");
    assertThat(applicantTiClient.getCountryCode().get()).isEqualTo("US");
    assertThat(applicantTiClient.getEmailAddress()).isEmpty();

    applicantEmailOnly.refresh();
    assertThat(applicantEmailOnly.getEmailAddress().get())
        .isEqualTo("picard_account_1@starfleet.com");
    assertThat(applicantEmailOnly.getFirstName()).isEmpty();
    assertThat(applicantEmailOnly.getMiddleName()).isEmpty();
    assertThat(applicantEmailOnly.getLastName()).isEmpty();
    assertThat(applicantEmailOnly.getDateOfBirth()).isEmpty();
    assertThat(applicantEmailOnly.getPhoneNumber()).isEmpty();
    assertThat(applicantEmailOnly.getCountryCode()).isEmpty();

    applicantEmailPresent.refresh();
    assertThat(applicantEmailPresent.getEmailAddress().get())
        .isEqualTo("picard_real_email@starfleet.com");
    assertThat(applicantEmailPresent.getFirstName()).isEmpty();
    assertThat(applicantEmailPresent.getMiddleName()).isEmpty();
    assertThat(applicantEmailPresent.getLastName()).isEmpty();
    assertThat(applicantEmailPresent.getDateOfBirth()).isEmpty();
    assertThat(applicantEmailPresent.getPhoneNumber()).isEmpty();
    assertThat(applicantEmailPresent.getCountryCode()).isEmpty();
  }
}
