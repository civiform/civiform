package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import durablejobs.DurableJobName;
import java.time.Instant;
import java.util.Optional;
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
import repository.ApplicationEventRepository;
import repository.ResetPostgres;
import services.DateConverter;
import services.application.ApplicationEventDetails;

public class UpdateLastActivityTimeForAccountsTest extends ResetPostgres {

  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.UPDATE_LAST_ACTIVITY_TIME_FOR_ACCOUNTS_20250825.toString(),
          JobType.RUN_ONCE,
          Instant.now());
  private DateConverter dateConverter;
  private ApplicationEventRepository repo;

  @Before
  public void setUp() {
    dateConverter = instanceOf(DateConverter.class);
    repo = instanceOf(ApplicationEventRepository.class);
  }

  @Test
  public void run_LastActivityTimeForAccounts_populatesAccountWithApplicantCreateTime()
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
  public void run_LastActivityTimeForAccounts_populatesWithApplicationSubmitTime()
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
  public void run_LastActivityTimeForAccounts_populatesWithApplicationCreateTime()
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
  public void run_LastActivityTimeForAccounts_populatesWithDefaultTimeWhenNoApplicantPresent()
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

  @Test
  public void
      run_LastActivityTimeForAccounts_populatesWithLatestActivityTimeWhenAllTimesArePresent()
          throws InterruptedException {
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    application.setSubmitTimeToNow();
    application.refresh();

    repo.insertStatusEvent(
            application,
            Optional.empty(),
            ApplicationEventDetails.StatusEvent.builder()
                .setStatusText("Status")
                .setEmailSent(false)
                .build())
        .toCompletableFuture()
        .join();
    application.refresh();
    resourceCreator.setLastActivityTimeToNull();
    TimeUnit.MILLISECONDS.sleep(5);
    applicant.getAccount().refresh();

    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    applicant.getAccount().refresh();
    var timeAfterUpdate = applicant.getAccount().getLastActivityTime();
    Instant statusLastModifiedTime = application.getStatusLastModifiedTime().get();
    assertThat(timeAfterUpdate).isEqualTo(statusLastModifiedTime);
  }

  @Test
  public void run_LastActivityTimeForAccounts_DoesnotUpdateWhenLastModifiedIsPresent()
      throws InterruptedException {
    ProgramModel program = resourceCreator.insertActiveProgram("Program");
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    application.setSubmitTimeToNow();
    application.refresh();

    repo.insertStatusEvent(
            application,
            Optional.empty(),
            ApplicationEventDetails.StatusEvent.builder()
                .setStatusText("Status")
                .setEmailSent(false)
                .build())
        .toCompletableFuture()
        .join();
    application.refresh();
    TimeUnit.MILLISECONDS.sleep(5);
    applicant.getAccount().refresh();
    var timeBeforeJobRun = applicant.getAccount().getLastActivityTime();

    // run job
    UpdateLastActivityTimeForAccounts job = new UpdateLastActivityTimeForAccounts(jobModel);
    job.run();

    // verify
    applicant.getAccount().refresh();
    var timeAfterJobRun = applicant.getAccount().getLastActivityTime();
    assertThat(timeAfterJobRun).isEqualTo(timeBeforeJobRun);
  }
}
