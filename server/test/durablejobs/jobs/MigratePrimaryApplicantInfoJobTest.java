package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import durablejobs.DurableJobName;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
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

  private ApplicantModel createApplicantWithWellKnownPathData(boolean withPaiData) {
    Database database = DB.getDefault();
    Transaction transaction = database.beginTransaction();
    AccountModel account = new AccountModel();
    account.setEmailAddress("account@email.com");
    account.save();
    ApplicantModel applicant = new ApplicantModel();
    applicant.setAccount(account);
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
    transaction.commit();
    transaction.close();
    return applicant;
  }

  private void runJob() {
    runJob(false);
  }

  private void runJob(boolean setFlag) {
    Database database = DB.getDefault();
    Transaction transaction = database.beginTransaction();
    // Flag will be false by default
    SettingsService settingsService = instanceOf(SettingsService.class);
    if (setFlag) {
      ImmutableMap<String, String> settings =
          settingsService.loadSettings().toCompletableFuture().join().get();
      ImmutableMap.Builder<String, String> newSettings = ImmutableMap.builder();
      for (var entry : settings.entrySet()) {
        if (!entry.getKey().equals("PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED")) {
          newSettings.put(entry);
        }
      }
      newSettings.put("PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED", "true");
      settingsService.updateSettings(newSettings.build(), "test");
      transaction.commit();
      transaction = database.beginTransaction();
    }
    PersistedDurableJobModel job =
        new PersistedDurableJobModel(
            DurableJobName.MIGRATE_PRIMARY_APPLICANT_INFO.toString(), Instant.ofEpochMilli(0));
    MigratePrimaryApplicantInfoJob migrateJob =
        new MigratePrimaryApplicantInfoJob(
            job, instanceOf(AccountRepository.class), settingsService, instanceOf(Config.class));
    migrateJob.run();
    transaction.commit();
    transaction.close();
  }

  @Test
  public void run_doesNotMigrateDataWhenFlagIsOn() {
    ApplicantModel applicant = createApplicantWithWellKnownPathData(/* withPaiData= */ true);
    runJob(/* setFlag= */ true);
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
}
