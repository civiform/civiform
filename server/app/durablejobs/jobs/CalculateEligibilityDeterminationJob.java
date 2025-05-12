package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import javax.inject.Inject;
import models.ApplicationModel;
import models.EligibilityDetermination;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramDefinition;

/**
 * Calculate the eligibility determination for applications submitted before the pre-compute
 * eligibility feature was implemented.
 */
public final class CalculateEligibilityDeterminationJob extends DurableJob {
  private static final Logger logger =
      LoggerFactory.getLogger(CalculateEligibilityDeterminationJob.class);
  private final ApplicantService applicantService;

  private final Database database;
  private final PersistedDurableJobModel persistedDurableJobModel;

  @Inject
  public CalculateEligibilityDeterminationJob(
      ApplicantService applicantService, PersistedDurableJobModel persistedDurableJobModel) {
    this.applicantService = checkNotNull(applicantService);
    this.persistedDurableJobModel = checkNotNull(persistedDurableJobModel);
    this.database = DB.getDefault();
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    logger.info("Starting job to calculate eligibility determination.");

    try (Transaction jobTransaction = database.beginTransaction(TxIsolation.SERIALIZABLE)) {
      jobTransaction.setBatchMode(true);
      int errorCount = 0;

      String filter =
          """
          eligibility_determination = 'NOT_COMPUTED' AND lifecycle_stage = 'active'
          """;
      try (var query = database.find(ApplicationModel.class).where().raw(filter).findIterate()) {
        while (query.hasNext() && errorCount < 10) {
          try {
            ApplicationModel application = query.next();
            logger.info(
                "Calculating eligibility determination for application id {}", application.id);
            ProgramModel pm = application.getProgram();
            ProgramDefinition programDefinition = pm.getProgramDefinition();
            ReadOnlyApplicantProgramService roAppProgramService =
                applicantService.getReadOnlyApplicantProgramService(application, programDefinition);
            EligibilityDetermination eligibilityDetermination =
                applicantService.calculateEligibilityDetermination(
                    programDefinition, roAppProgramService);
            application.setEligibilityDetermination(eligibilityDetermination);
            application.save();
          } catch (RuntimeException e) {
            errorCount++;
            logger.error("Error message {}", e.getMessage());
          }
        }
      }

      if (errorCount == 0) {
        jobTransaction.commit();
        logger.info("Job succeeded");
      } else {
        logger.error(
            "Failed to compute eligibility determination for existing applications. Error: {}",
            errorCount);
        jobTransaction.rollback();
      }
    }
  }
}
