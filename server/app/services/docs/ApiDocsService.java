package services.docs;

import static services.export.JsonPrettifier.asPrettyJsonString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Inject;
import models.LifecycleStage;
import repository.ExportServiceRepository;
import services.export.ProgramJsonSampler;
import services.program.ProgramDefinition;
import services.program.ProgramDraftNotFoundException;
import services.program.ProgramService;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;

/**
 * Service that encapsulates the service and repository calls required to render API docs and the
 * API schema viewer. Keeps mappers free of data access so they remain pure property mappers.
 */
public final class ApiDocsService {

  private final ProgramService programService;
  private final ProgramJsonSampler programJsonSampler;
  private final ExportServiceRepository exportServiceRepository;

  @Inject
  public ApiDocsService(
      ProgramService programService,
      ProgramJsonSampler programJsonSampler,
      ExportServiceRepository exportServiceRepository) {
    this.programService = programService;
    this.programJsonSampler = programJsonSampler;
    this.exportServiceRepository = exportServiceRepository;
  }

  public ImmutableSet<String> getAllNonExternalProgramSlugs() {
    return programService.getAllNonExternalProgramSlugs();
  }

  /**
   * Loads the program definition for the given slug at the given lifecycle stage, excluding
   * external programs. Only {@link LifecycleStage#ACTIVE} and {@link LifecycleStage#DRAFT} are
   * supported; any other stage returns {@link Optional#empty()}.
   */
  public Optional<ProgramDefinition> getProgramDefinition(
      String programSlug, LifecycleStage lifecycleStage) {
    if (!getAllNonExternalProgramSlugs().contains(programSlug)) {
      return Optional.empty();
    }
    try {
      return switch (lifecycleStage) {
        case ACTIVE ->
            Optional.of(
                programService
                    .getActiveFullProgramDefinitionAsync(programSlug)
                    .toCompletableFuture()
                    .join());
        case DRAFT -> Optional.of(programService.getDraftFullProgramDefinition(programSlug));
        default -> Optional.empty();
      };
    } catch (RuntimeException | ProgramDraftNotFoundException e) {
      return Optional.empty();
    }
  }

  /** Returns a pretty-printed JSON sample of the program's API response. */
  public String getSampleJsonPreview(ProgramDefinition programDefinition) {
    return asPrettyJsonString(programJsonSampler.getSampleJson(programDefinition));
  }

  /**
   * Returns the historic multi-option admin names for every multi-option question in the program,
   * keyed by the question's name key.
   */
  public ImmutableMap<String, ImmutableList<String>> getHistoricOptionsByQuestionNameKey(
      ProgramDefinition programDefinition) {
    return programDefinition
        .streamQuestionDefinitions()
        .filter(qd -> qd.getQuestionType().isMultiOptionType())
        .collect(
            ImmutableMap.toImmutableMap(
                QuestionDefinition::getQuestionNameKey,
                qd ->
                    exportServiceRepository.getAllHistoricMultiOptionAdminNames(
                        (MultiOptionQuestionDefinition) qd)));
  }
}
