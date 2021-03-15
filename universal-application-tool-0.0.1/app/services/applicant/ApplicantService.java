package services.applicant;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletionStage;
import models.Applicant;
import services.ErrorAnd;

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
      long applicantId, long programId, long blockId, ImmutableSet<Update> updates);

  /**
   * Equivalent to the other {@link ApplicantService#stageAndUpdateIfValid(long, long, long,
   * ImmutableSet<Update>)}, but takes a map representing the {@link Update}s.
   */
  CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, Exception>> stageAndUpdateIfValid(
      long applicantId, long programId, long blockId, ImmutableMap<String, String> updateMap);

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
}
