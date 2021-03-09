package services.applicant;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletionStage;
import models.Applicant;
import services.ErrorAnd;

/** Defines the interface facade for Applicant service */
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
   */
  CompletionStage<ReadOnlyApplicantProgramService> stageAndUpdateIfValid(
      long applicantId, long programId, long blockId, ImmutableSet<Update> updates);

  /**
   * Equivalent to the other {@link ApplicantService#stageAndUpdateIfValid(long, long, long,
   * ImmutableSet<Update>)}, but takes a map representing the {@link Update}s.
   */
  CompletionStage<ReadOnlyApplicantProgramService> stageAndUpdateIfValid(
      long applicantId, long programId, long blockId, ImmutableMap<String, String> updateMap);

  /** Creates a new {@link models.Applicant} at for latest application version for a given user. */
  CompletionStage<Applicant> createApplicant(long userId);

  /**
   * Get a {@link ReadOnlyApplicantProgramService} which implements synchronous, in-memory read
   * behavior relevant to an applicant for a specific program.
   */
  CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId);
}
