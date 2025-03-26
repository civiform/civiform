package durablejobs.jobs;

import static org.checkerframwork.errorprone.com.google.common.base.Preconditions.checkNotNull;

import durablejobs.DurableJob;
import io.ebean.Database;
import io.ebean.Transaction;
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
 * Calculate eligibility determination for application submitted before pre-compute eligibility
 * feature was implemented.
 */
public final class CalculateEligibilityDeterminationJob extends DurableJob {
  private static final Logger logger =
      LoggerFactory.getLogger(CalculateEligibilityDeterminationJob.class);
  private final ApplicantService applicantService;

  private final Database database;

  @Inject
  public CalculateEligibilityDeterminationJob(ApplicantService applicantService) {
    this.applicantService = checkNotNull(applicantService);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getPersistedDurableJob'");
  }

  @Override
  public void run() {
    logger.info("Starting job to calculate eligibility determination.");

    try (Transaction jobTransaction = database.beginTransaction()) {
      String filter =
          """
           eligibility_determination = NOT_COMPUTED
          """;
      try (var query = database.find(ApplicationModel.class).where().raw(filter).findIterate()) {
        while (query.hasNext()) {
          try {
            ApplicationModel application = query.next();
            logger.debug(
                "Calculating eligibility determination for application id {}", application.id);
            ProgramModel pm = application.getProgram();
            ProgramDefinition programDefinition = pm.getProgramDefinition();
            ReadOnlyApplicantProgramService roAppProgramService =
                applicantService.getReadOnlyApplicantProgramService(application, programDefinition);
            EligibilityDetermination eligibilityDetermination =
                applicantService.calculateEligibilityDetermination(
                    programDefinition, roAppProgramService);
            application.setEligibilityDetermination(eligibilityDetermination);
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      }
    }
  }
}
