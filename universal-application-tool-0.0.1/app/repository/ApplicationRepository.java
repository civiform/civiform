package repository;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Application;
import models.Program;

public class ApplicationRepository {
  private final ProgramRepository programRepository;
  private final ApplicantRepository applicantRepository;
  private final Clock clock;

  @Inject
  public ApplicationRepository(
      ProgramRepository programRepository, ApplicantRepository applicantRepository, Clock clock) {
    this.programRepository = programRepository;
    this.applicantRepository = applicantRepository;
    this.clock = clock;
  }

  public CompletionStage<Application> submitApplication(Applicant applicant, Program program) {
    return supplyAsync(
        () -> {
          Application application = new Application(applicant, program, clock.instant());
          application.save();
          return application;
        });
  }

  public CompletionStage<Optional<Application>> submitApplication(
      long applicantId, long programId) {
    CompletionStage<Optional<Applicant>> applicantDb =
        applicantRepository.lookupApplicant(applicantId);
    CompletionStage<Optional<Program>> programDb = programRepository.lookupProgram(programId);
    return applicantDb.thenCombineAsync(
        programDb,
        (applicantMaybe, programMaybe) -> {
          if (applicantMaybe.isEmpty() || programMaybe.isEmpty()) {
            return Optional.empty();
          }
          Applicant applicant = applicantMaybe.get();
          Program program = programMaybe.get();
          Application application = new Application(applicant, program, clock.instant());
          application.save();
          return Optional.of(application);
        });
  }
}
