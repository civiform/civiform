package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import durablejobs.DurableJobName;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.time.Instant;
import models.AccountModel;
import models.ApplicantModel;
import models.JobType;
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
    Transaction transaction = database.beginTransaction();
    AccountModel account = new AccountModel();
    account.save();
    ApplicantModel applicant = new ApplicantModel();
    applicant.setAccount(account);
    applicant.save();
    transaction.commit();
    return applicant;
  }

  private ApplicantModel createApplicantWithWellKnownPathData(boolean withPaiData) {
    ApplicantModel applicant = createApplicant();
    Transaction transaction = database.beginTransaction();
    AccountModel account = applicant.getAccount();
    account.setEmailAddress("account@email.com");
    account.save();
    ApplicantData applicantData = applicant.getApplicantData();
    applicantData.putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    applicantData.putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, "Luc");
    applicantData.putString(WellKnownPaths.APPLICANT_LAST_NAME, "Picard");
    applicantData.putString(WellKnownPaths.APPLICANT_NAME_SUFFIX, "II.");
    applicantData.putDate(WellKnownPaths.APPLICANT_DOB, "2305-07-13");
    applicantData.putString(WellKnownPaths.APPLICANT_PHONE_NUMBER, "5038234000");
    if (withPaiData) {
      applicant.setFirstName("Kathryn");
      applicant.setMiddleName("");
      applicant.setLastName("Janeway");
      applicant.setSuffix("");
      applicant.setDateOfBirth("2328-05-20");
      applicant.setPhoneNumber("2066842489");
      applicant.setEmailAddress("applicant@email.com");
    }
    applicant.save();
    transaction.commit();
    return applicant;
  }

  private void runJob() {
    runJob(/* paiFlagEnabled= */ false);
  }

  private void runJob(Boolean paiFlagEnabled) {
    Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE);

    SettingsService settingsService = instanceOf(SettingsService.class);
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of("primary_applicant_info_questions_enabled", paiFlagEnabled.toString()));

    PersistedDurableJobModel job =
        new PersistedDurableJobModel(
            DurableJobName.MIGRATE_PRIMARY_APPLICANT_INFO.toString(),
            JobType.RECURRING,
            Instant.ofEpochMilli(0));
    MigratePrimaryApplicantInfoJob migrateJob =
        new MigratePrimaryApplicantInfoJob(
            job, instanceOf(AccountRepository.class), settingsService, config);
    migrateJob.run();
    transaction.commit();
  }

  @Test
  public void run_doesNotMigrateDataWhenFlagIsOn() {
    ApplicantModel applicant = createApplicantWithWellKnownPathData(/* withPaiData= */ true);
    runJob(/* paiFlagEnabled= */ true);
    applicant.refresh();
    assertThat(applicant.getFirstName().get()).isEqualTo("Kathryn");
    assertThat(applicant.getMiddleName()).isEmpty();
    assertThat(applicant.getLastName().get()).isEqualTo("Janeway");
    assertThat(applicant.getSuffix()).isEmpty();
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
    assertThat(applicant.getSuffix().get()).isEqualTo("II.");
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
    assertThat(applicant.getSuffix().get()).isEqualTo("II.");
    assertThat(applicant.getDateOfBirth().get()).isEqualTo("2305-07-13");
    assertThat(applicant.getPhoneNumber().get()).isEqualTo("5038234000");
    assertThat(applicant.getEmailAddress().get()).isEqualTo("applicant@email.com");
  }

  @Test
  public void run_migratesWhenOnlyFirstNameIsPopulated() {
    ApplicantModel applicant = createApplicant();
    Transaction transaction = database.beginTransaction();
    applicant.getApplicantData().putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    applicant.save();
    transaction.commit();

    runJob();

    applicant.refresh();
    assertThat(applicant.getFirstName().get()).isEqualTo("Jean");
    assertThat(applicant.getMiddleName()).isEmpty();
    assertThat(applicant.getLastName()).isEmpty();
    assertThat(applicant.getSuffix()).isEmpty();
    assertThat(applicant.getDateOfBirth()).isEmpty();
    assertThat(applicant.getEmailAddress()).isEmpty();
    assertThat(applicant.getPhoneNumber()).isEmpty();
    assertThat(applicant.getCountryCode()).isEmpty();
  }

  @Test
  public void run_migratesWhenTiClientDataIsPopulated() {
    ApplicantModel applicant = createApplicant();
    Transaction transaction = database.beginTransaction();
    ApplicantData data = applicant.getApplicantData();
    data.putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    data.putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, "Luc");
    data.putString(WellKnownPaths.APPLICANT_LAST_NAME, "Picard");
    data.putString(WellKnownPaths.APPLICANT_NAME_SUFFIX, "II.");
    data.putDate(WellKnownPaths.APPLICANT_DOB, "2305-07-13");
    data.putString(WellKnownPaths.APPLICANT_PHONE_NUMBER, "5038234000");
    applicant.save();
    transaction.commit();

    runJob();

    applicant.refresh();
    assertThat(applicant.getFirstName().get()).isEqualTo("Jean");
    assertThat(applicant.getMiddleName().get()).isEqualTo("Luc");
    assertThat(applicant.getLastName().get()).isEqualTo("Picard");
    assertThat(applicant.getSuffix().get()).isEqualTo("II.");
    assertThat(applicant.getDateOfBirth().get()).isEqualTo("2305-07-13");
    assertThat(applicant.getPhoneNumber().get()).isEqualTo("5038234000");
    assertThat(applicant.getCountryCode().get()).isEqualTo("US");
    assertThat(applicant.getEmailAddress()).isEmpty();
  }

  @Test
  public void run_migratesWhenOnlyAccountEmailIsPresent() {
    ApplicantModel applicant = createApplicant();
    Transaction transaction = database.beginTransaction();
    AccountModel account = applicant.getAccount();
    account.setEmailAddress("picard@starfleet.com");
    account.save();
    transaction.commit();

    runJob();

    applicant.refresh();
    assertThat(applicant.getEmailAddress().get()).isEqualTo("picard@starfleet.com");
    assertThat(applicant.getFirstName()).isEmpty();
    assertThat(applicant.getMiddleName()).isEmpty();
    assertThat(applicant.getLastName()).isEmpty();
    assertThat(applicant.getSuffix()).isEmpty();
    assertThat(applicant.getDateOfBirth()).isEmpty();
    assertThat(applicant.getPhoneNumber()).isEmpty();
    assertThat(applicant.getCountryCode()).isEmpty();
  }

  @Test
  public void run_doesNotMigrateWhenEmailIsAlreadyPresent() {
    ApplicantModel applicant = createApplicant();
    Transaction transaction = database.beginTransaction();
    AccountModel account = applicant.getAccount();
    account.setEmailAddress("picard@starfleet.com");
    account.save();
    applicant.setEmailAddress("picard_real_email@starfleet.com");
    applicant.save();
    transaction.commit();

    runJob();

    applicant.refresh();
    assertThat(applicant.getEmailAddress().get()).isEqualTo("picard_real_email@starfleet.com");
    assertThat(applicant.getFirstName()).isEmpty();
    assertThat(applicant.getMiddleName()).isEmpty();
    assertThat(applicant.getLastName()).isEmpty();
    assertThat(applicant.getSuffix()).isEmpty();
    assertThat(applicant.getDateOfBirth()).isEmpty();
    assertThat(applicant.getPhoneNumber()).isEmpty();
    assertThat(applicant.getCountryCode()).isEmpty();
  }
}
