package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicantRepository;
import services.ErrorAnd;
import services.program.BlockDefinition;
import services.program.PathNotInBlockException;
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
  public CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, String>> stageAndUpdateIfValid(
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

              ImmutableSet<String> updateErrors =
                  stageUpdates(applicant, programDefinition, blockId, updates);
              if (!updateErrors.isEmpty()) {
                return CompletableFuture.completedFuture(ErrorAnd.error(updateErrors));
              }

              ReadOnlyApplicantProgramService roApplicantProgramService =
                  new ReadOnlyApplicantProgramServiceImpl(
                      applicant.getApplicantData(), programDefinition);

              Optional<Block> blockMaybe = roApplicantProgramService.getBlock(blockId);
              if (blockMaybe.get().isValid()) {
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

  /**
   * Tries to perform an in-place update of {@link ApplicantData}. Returns error messages if it is
   * impossible to perform the operation.
   */
  private ImmutableSet<String> stageUpdates(
      Applicant applicant,
      ProgramDefinition programDefinition,
      long blockId,
      ImmutableSet<Update> updates) {

    Optional<BlockDefinition> blockDefinitionMaybe = programDefinition.getBlockDefinition(blockId);
    if (blockDefinitionMaybe.isEmpty()) {
      return ImmutableSet.of("Block not found.");
    }
    BlockDefinition blockDefinition = blockDefinitionMaybe.get();

    ImmutableList<Path> updatePaths =
        updates.stream().map(Update::path).collect(ImmutableList.toImmutableList());
    if (!blockDefinition.hasPaths(updatePaths)) {
      return ImmutableSet.of("Not all paths exist in this Block.");
    }

    try {
      stageUpdates(applicant.getApplicantData(), blockDefinition, updates);
    } catch (UnsupportedScalarTypeException e) {
      return ImmutableSet.of("Unrecognized scalar type");
    } catch (PathNotInBlockException e) {
      return ImmutableSet.of("Update path does not exist.");
    }

    return ImmutableSet.of();
  }

  /** In-place update of applicant data. */
  private void stageUpdates(
      ApplicantData applicantData, BlockDefinition blockDefinition, ImmutableSet<Update> updates)
      throws UnsupportedScalarTypeException, PathNotInBlockException {
    for (Update update : updates) {
      Optional<ScalarType> typeMaybe = blockDefinition.getScalarType(update.path());
      if (typeMaybe.isEmpty()) {
        throw new PathNotInBlockException(blockDefinition, update.path());
      }
      switch (typeMaybe.get()) {
        case STRING:
          applicantData.putString(update.path(), (String) update.value());
          break;
        case INT:
          applicantData.putInteger(update.path(), (Integer) update.value());
          break;
        default:
          throw new UnsupportedScalarTypeException(typeMaybe.get());
      }
    }
  }
}
