package services.applicant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletionStage;
import models.Applicant;
import models.Application;
import services.ErrorAnd;
import services.program.ProgramDefinition;

/**
 * The service responsible for accessing the Applicant resource. Applicants can view program
 * applications defined by the {@link services.program.ProgramService} as a series of {@link
 * Block}s, one per-page. When an applicant submits the form for a Block the ApplicantService is
 * responsible for validating and persisting their answers and then providing the next Block for
 * them to view, if any.
 */
public interface ApplicantService {

  /**
   * Attempts to perform a set of updates to the applicant's {@link ApplicantData}. If updates are
   * valid, they are saved to storage.
   *
   * <p>Updates are atomic i.e. if any of them fail validation none of them will be written.
   *
   * @return a {@link ReadOnlyApplicantProgramService} that reflects the updates, which may have
   *     invalid data with errors associated with it. If the service cannot perform the update, an
   *     {@link ErrorAnd} is returned in the error state.
   *     <p>A ProgramNotFoundException may be thrown when the future completes if the programId does
   *     not correspond to a real Program.
   */
  CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, Exception>> stageAndUpdateIfValid(
      long applicantId, long programId, String blockId, ImmutableMap<String, String> updateMap);

  /** Creates a new {@link models.Applicant} at for latest application version for a given user. */
  CompletionStage<Applicant> createApplicant(long userId);

  /**
   * Get a {@link ReadOnlyApplicantProgramService} which implements synchronous, in-memory read
   * behavior relevant to an applicant for a specific program.
   *
   * <p>A ProgramNotFoundException may be thrown when the future completes if the programId does not
   * correspond to a real Program.
   */
  CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId);

  String applicantName(Application application);

  /**
   * Returns all programs that are appropriate to serve to an applicant - which is any active
   * program, plus any program where they have an application in the draft stage.
   */
  CompletionStage<ImmutableList<ProgramDefinition>> relevantPrograms(long applicantId);
}
