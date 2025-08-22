package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.util.Optional;
import javax.inject.Inject;
import models.ApplicationModel;
import models.EligibilityDetermination;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/**
 * Calculate the eligibility determination for applications submitted before the pre-compute
 * eligibility feature was implemented.
 */
public final class CalculateEligibilityDeterminationJob extends DurableJob {
  private static final Logger logger =
      LoggerFactory.getLogger(CalculateEligibilityDeterminationJob.class);
  private final ApplicantService applicantService;
  private final ProgramService programService;

  private final Database database;
  private final PersistedDurableJobModel persistedDurableJobModel;

  @Inject
  public CalculateEligibilityDeterminationJob(
      ApplicantService applicantService,
      ProgramService programService,
      PersistedDurableJobModel persistedDurableJobModel) {
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
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
      jobTransaction.setBatchSize(50);
      int errorCount = 0;

      try (var query =
          database
              .find(ApplicationModel.class)
              .where()
              .eq("eligibility_determination", EligibilityDetermination.NOT_COMPUTED)
              .eq("lifecycle_stage", "active")
              .findIterate()) {
        while (query.hasNext() && errorCount < 10) {
          Optional<ApplicationModel> applicationOptional = Optional.empty();
          Long currentApplicationId = null;
          try {
            applicationOptional = Optional.ofNullable(query.next());
            if (applicationOptional.isPresent()) {
              ApplicationModel application = applicationOptional.get();
              currentApplicationId = application.id; // Capture the ID here
              logger.info(
                  "Calculating eligibility determination for application id {}",
                  application.id); // Use the captured ID

              Long programId = application.getProgram().id;
              ProgramDefinition programDefinition =
                  programService.getFullProgramDefinition(programId);

              ReadOnlyApplicantProgramService roAppProgramService =
                  applicantService.getReadOnlyApplicantProgramService(
                      application, programDefinition);

              EligibilityDetermination eligibilityDetermination =
                  applicantService.calculateEligibilityDetermination(
                      programDefinition, roAppProgramService);
              application.setEligibilityDetermination(eligibilityDetermination);
              application.save();
            } else {
              logger.warn("Query returned a null application despite hasNext() being true.");
              errorCount++;
            }
          } catch (RuntimeException | ProgramNotFoundException e) {
            errorCount++;
            final Long finalApplicationId = currentApplicationId;
            applicationOptional.ifPresentOrElse(
                app ->
                    logger.error(
                        "Error processing application ID {}: {}",
                        finalApplicationId != null ? finalApplicationId : "unknown",
                        e.getMessage()),
                () -> logger.error("Error message: {}", e.getMessage()));
          }
        }
      }

      if (errorCount == 0) {
        jobTransaction.commit();
        logger.info("Eligibility Determination: job successful");
      } else {
        String errorMessage =
            String.format("Eligibility Determination: stopping early after %d errors", errorCount);
        logger.error(errorMessage);
        persistedDurableJobModel.appendErrorMessage(errorMessage);
        jobTransaction.rollback();
      }
    }
  }
}
