package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import durablejobs.DurableJobName;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.JobType;
import models.LifecycleStage;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.DateConverter;

public class UpdateLastActivityTimeForAccountsTest extends ResetPostgres {

  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.UPDATE_LAST_ACTIVITY_TIME_FOR_ACCOUNTS.toString(),
          JobType.RUN_ONCE,
          Instant.now());
  private DateConverter dateConverter;

  @Before
  public void setUp() {
    dateConverter = instanceOf(DateConverter.class);
  }

  @Test
  public void run_LastActivityTimeForAccounts_Populates_AccountWithApplicantCreateTime()
      throws InterruptedException {
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    var timeBeforeUpdate = applicant.getAccount().getLastActivityTime();
    resourceCreator.setLastActivityTimeToNull();

    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    applicant.getAccount().refresh();
    var timeAfterUpdate = applicant.getAccount().getLastActivityTime();
    assertThat(timeAfterUpdate).isNotEqualTo(timeBeforeUpdate);
  }

  @Test
  public void run_LastActivityTimeForAccounts_Populates_With_ApplicationSubmitTime()
      throws InterruptedException {
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    AccountModel account = applicant.getAccount();
    ProgramModel program = resourceCreator.insertActiveProgram("FreshBucks");
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    application.setSubmitTimeToNow();
    application.save();
    resourceCreator.setLastActivityTimeToNull();
    application.refresh();
    var applicationSubmitTime = application.getSubmitTime();

    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    account.refresh();
    var timeAfterJob = account.getLastActivityTime();
    assertThat(applicationSubmitTime).isEqualTo(timeAfterJob);
  }

  @Test
  public void run_LastActivityTimeForAccounts_Populates_With_ApplicantCreateTime()
      throws InterruptedException {
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    var ApplicantCreateTime = applicant.getWhenCreated();
    resourceCreator.setLastActivityTimeToNull();
    TimeUnit.MILLISECONDS.sleep(5);
    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    applicant.getAccount().refresh();
    var timeAfterUpdate = applicant.getAccount().getLastActivityTime();
    assertThat(ApplicantCreateTime).isEqualTo(timeAfterUpdate);
  }

  @Test
  public void run_LastActivityTimeForAccounts_Populates_With_ApplicationCreateTime()
      throws InterruptedException {
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ProgramModel program = resourceCreator.insertActiveProgram("FreshBucks");
    ApplicationModel application =
        resourceCreator.insertApplication(applicant, program, LifecycleStage.DRAFT);
    var applicationCreateTime = application.getCreateTime();

    resourceCreator.setLastActivityTimeToNull();
    TimeUnit.MILLISECONDS.sleep(5);
    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    applicant.getAccount().refresh();
    var timeAfterJob = applicant.getAccount().getLastActivityTime();
    assertThat(applicationCreateTime).isEqualTo(timeAfterJob);
  }

  @Test
  public void run_LastActivityTimeForAccounts_Populates_WithNoApplicant()
      throws InterruptedException {
    AccountModel account = resourceCreator.insertAccount();
    resourceCreator.setLastActivityTimeToNull();
    account.refresh();
    TimeUnit.MILLISECONDS.sleep(5);
    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    account.refresh();
    var timeAfterUpdate = account.getLastActivityTime();
    Instant defaultTime = dateConverter.parseIso8601DateToStartOfLocalDateInstant("1900-01-01");
    assertThat(timeAfterUpdate).isNotEqualTo(defaultTime);
  }
}
