package services.applicant;

import java.util.concurrent.CompletionStage;
import models.Applicant;
import services.ErrorAnd;

interface ApplicantService {
  CompletionStage<ErrorAnd<ReadOnlyApplicantService, UpdateError>> update(
      long applicantId,
      long programId,
      ImmutableList<Update> updates);

  CompletionStage<ReadOnlyApplicantService> getReadOnlyApplicantService(long applicantId, long programId);

  CompletionStage<Applicant> createApplicant(long userId);
}
