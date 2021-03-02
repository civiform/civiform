package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicantRepository;
import services.ErrorAnd;
import services.program.ProgramDefinition;
import services.program.ProgramService;

public class ApplicantServiceImpl implements ApplicantService {

  private final ApplicantRepository applicantRepository;
  private final ProgramService programService;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ApplicantServiceImpl(
      ApplicantRepository applicantRepository,
      ProgramService programService,
      HttpExecutionContext httpExecutionContext) {
    this.applicantRepository = checkNotNull(applicantRepository);
    this.programService = checkNotNull(programService);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  @Override
  public CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, UpdateError>> update(
      long applicantId, long programId, ImmutableSet<Update> updates) {
    // ApplicantData applicantData = applicant.getApplicantData();
    // Updater updater = new Updater(applicantData)
    // updater.update(updates)
    // ImmutableSet<UpdateError> errors = updater.validate()
    //
    // if errors.isEmpty()
    //   updater.save()
    //   return the ro app program service (no errors)
    // else
    //   return the ro app service and the set of errors

    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        applicantRepository.lookupApplicant(applicantId).toCompletableFuture();
    CompletableFuture<Optional<ProgramDefinition>> programDefinitionCompletableFuture =
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenApplyAsync(
            (v) -> {
              Applicant applicant = applicantCompletableFuture.join().get();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join().get();

              ReadOnlyApplicantProgramService roApplicantProgramService =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition);
              return new ErrorAnd<>(roApplicantProgramService);
            },
            httpExecutionContext.current());
  }

  @Override
  public CompletionStage<Applicant> createApplicant(long userId) {
    Applicant applicant = new Applicant();
    return applicantRepository.insertApplicant(applicant).thenApply((unused) -> applicant);
  }

  @Override
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        applicantRepository.lookupApplicant(applicantId).toCompletableFuture();
    CompletableFuture<Optional<ProgramDefinition>> programDefinitionCompletableFuture =
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenApplyAsync(
            (v) -> {
              Applicant applicant = applicantCompletableFuture.join().get();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join().get();

              return new ReadOnlyApplicantProgramServiceImpl(
                  applicant.getApplicantData(), programDefinition);
            },
            httpExecutionContext.current());
  }
}
