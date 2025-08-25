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
import org.junit.Test;
import repository.ResetPostgres;

public class UpdateLastActivityTimeForAccountsTest extends ResetPostgres {

  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.UPDATE_LAST_ACTIVITY_TIME_FOR_ACCOUNTS.toString(),
          JobType.RUN_ONCE,
          Instant.now());

  @Test
  public void run_LastActivityTimeForAccounts_Populates_AccountWithApplicantCreateTime()
      throws InterruptedException {
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    var timeBeforeUpdate = applicant.getAccount().getLastActivityTime();
    resourceCreator.setLastActivityTimeToNull(applicant.getAccount());
    TimeUnit.MILLISECONDS.sleep(5);
    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    var timeAfterUpdate = applicant.getAccount().getLastActivityTime();
    assertThat(timeAfterUpdate).isNotEqualTo(timeBeforeUpdate);
    assertThat(timeAfterUpdate).isAfter(timeBeforeUpdate);
  }

  @Test
  public void run_LastActivityTimeForAccounts_Populates_With_ApplicationSubmitTime()
      throws InterruptedException {
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ProgramModel program = resourceCreator.insertActiveProgram("FreshBucks");
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    resourceCreator.setLastActivityTimeToNull(applicant.getAccount());
    var applicationSubmitTime = application.getSubmitTime();

    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    var timeAfterJob = applicant.getAccount().getLastActivityTime();
    assertThat(applicationSubmitTime).isEqualTo(timeAfterJob);
  }

  @Test
  public void run_LastActivityTimeForAccounts_Populates_With_ApplicantCreateTime()
      throws InterruptedException {
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    var ApplicantCreateTime = applicant.getWhenCreated();
    resourceCreator.setLastActivityTimeToNull(applicant.getAccount());
    TimeUnit.MILLISECONDS.sleep(5);
    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
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
    resourceCreator.setLastActivityTimeToNull(applicant.getAccount());
    var applicationCreateTime = application.getCreateTime();

    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    var timeAfterJob = applicant.getAccount().getLastActivityTime();
    assertThat(applicationCreateTime).isEqualTo(timeAfterJob);
  }

  @Test
  public void run_LastActivityTimeForAccounts_Populates_WithNoApplicant()
      throws InterruptedException {
    AccountModel account = resourceCreator.insertAccount();
    resourceCreator.setLastActivityTimeToNull(account);
    TimeUnit.MILLISECONDS.sleep(5);
    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    var timeAfterUpdate = account.getLastActivityTime();
    assertThat(timeAfterUpdate).isNotEqualTo("1900-01-01 00:00:00");
  }
}
