package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicantRepository;
import services.ErrorAnd;
import services.Path;
import services.program.BlockDefinition;
import services.program.PathNotInBlockException;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.ScalarType;
import services.question.UnsupportedScalarTypeException;

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
  public CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, Exception>>
      stageAndUpdateIfValid(
          long applicantId, long programId, long blockId, ImmutableSet<Update> updates) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture =
        applicantRepository.lookupApplicant(applicantId).toCompletableFuture();

    CompletableFuture<Optional<ProgramDefinition>> programDefinitionCompletableFuture =
        programService.getProgramDefinitionAsync(programId).toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture)
        .thenComposeAsync(
            (v) -> {
              Applicant applicant = applicantCompletableFuture.join().get();
              ProgramDefinition programDefinition = programDefinitionCompletableFuture.join().get();

              try {
                stageUpdates(applicant, programDefinition, blockId, updates);
              } catch (ProgramBlockNotFoundException
                  | UnsupportedScalarTypeException
                  | PathNotInBlockException e) {
                return CompletableFuture.completedFuture(ErrorAnd.error(ImmutableSet.of(e)));
              }

              ReadOnlyApplicantProgramService roApplicantProgramService =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition);

              Optional<Block> blockMaybe = roApplicantProgramService.getBlock(blockId);
              if (!blockMaybe.get().hasErrors()) {
                return applicantRepository
                    .updateApplicant(applicant)
                    .thenApplyAsync(
                        (finishedSaving) -> ErrorAnd.of(roApplicantProgramService),
                        httpExecutionContext.current());
              }

              return CompletableFuture.completedFuture(ErrorAnd.of(roApplicantProgramService));
            },
            httpExecutionContext.current());
  }

  @Override
  public CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, Exception>>
      stageAndUpdateIfValid(
          long applicantId, long programId, long blockId, ImmutableMap<String, String> updateMap) {
    ImmutableSet<Update> updates =
        updateMap.entrySet().stream()
            .map(entry -> Update.create(Path.create(entry.getKey()), entry.getValue()))
            .collect(ImmutableSet.toImmutableSet());

    return stageAndUpdateIfValid(applicantId, programId, blockId, updates);
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

  /** In-place update of {@link Applicant}'s data. */
  private void stageUpdates(
      Applicant applicant,
      ProgramDefinition programDefinition,
      long blockId,
      ImmutableSet<Update> updates)
      throws ProgramBlockNotFoundException, UnsupportedScalarTypeException,
          PathNotInBlockException {

    BlockDefinition blockDefinition =
        programDefinition
            .getBlockDefinition(blockId)
            .orElseThrow(() -> new ProgramBlockNotFoundException(programDefinition.id(), blockId));
    stageUpdates(applicant.getApplicantData(), blockDefinition, updates);
  }

  /** In-place update of {@link ApplicantData}. */
  private void stageUpdates(
      ApplicantData applicantData, BlockDefinition blockDefinition, ImmutableSet<Update> updates)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    for (Update update : updates) {
      ScalarType type =
          blockDefinition
              .getScalarType(update.path())
              .orElseThrow(() -> new PathNotInBlockException(blockDefinition, update.path()));
      switch (type) {
        case STRING:
          applicantData.putString(update.path(), update.value());
          break;
        case INT:
          applicantData.putInteger(update.path(), Integer.valueOf(update.value()));
          break;
        default:
          throw new UnsupportedScalarTypeException(type);
      }
    }
  }
}
