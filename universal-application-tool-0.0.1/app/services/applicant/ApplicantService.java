package services.applicant;

import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletionStage;
import models.Applicant;
import services.ErrorAnd;

/** Defines the interface facade for Applicant service */
interface ApplicantService {

  /**
   * Performs a set of updates to the applicant's ApplicantData. Updates are atomic i.e. if any of
   * them fail validation none of them will be written. programId is used to construct the {@code
   * ReadOnlyApplicantService} provided in the return value.
   */
  CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, UpdateError>> update(
      long applicantId, long programId, ImmutableSet<Update> updates);

  /** Creates a new Applicant for a given user. */
  CompletionStage<Applicant> createApplicant(long userId);

  /**
   * Get a {@code ReadOnlyApplicantService} which implements synchronous, in-memory read behavior
   * relevant to an applicant for a specific program.
   */
  CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId);
}
