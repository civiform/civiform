package durablejobs.jobs;

import com.google.api.client.util.Preconditions;
import com.google.common.collect.ImmutableList;
import durablejobs.DurableJob;
import java.util.Optional;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import repository.ProgramRepository;
import repository.QuestionRepository;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;

/**
 * This is a temporary job that sets the new isEnumerator property to true on existing program block
 * definitions that have an enumerator question.
 */
public class SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob extends DurableJob {

  private final PersistedDurableJobModel persistedDurableJob;
  private final ProgramRepository programRepository;
  private final QuestionRepository questionRepository;

  public SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob(
      PersistedDurableJobModel persistedDurableJob,
      ProgramRepository programRepository,
      QuestionRepository questionRepository) {
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
    this.programRepository = Preconditions.checkNotNull(programRepository);
    this.questionRepository = Preconditions.checkNotNull(questionRepository);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    // Get the ids of all questions that have an enumerator type
    ImmutableList<Long> enumeratorQuestionIds =
        questionRepository.getAllQuestionIdsWithEnumeratorType();

    // Find any program where the block_definitions's questionDefinitions includes any of the
    // enumerator question ids
    ImmutableList<ProgramDefinition> programs =
        programRepository.getAllProgramsWithAnEnumeratorQuestion(enumeratorQuestionIds).stream()
            .map(ProgramModel::getProgramDefinition)
            .collect(ImmutableList.toImmutableList());

    // For each program with an enumerator question, find the blocks that have an enumerator
    // question and
    // update those block definitions to set isEnumerator = true
    programs.forEach(
        programDefinition -> {
          ImmutableList<BlockDefinition> updatedBlockDefinitions =
              programDefinition.blockDefinitions().stream()
                  .map(
                      blockDefinition ->
                          updateBlockIfEnumerator(blockDefinition, enumeratorQuestionIds))
                  .collect(ImmutableList.toImmutableList());

          ProgramDefinition updatedProgramDefinition =
              programDefinition.toBuilder().setBlockDefinitions(updatedBlockDefinitions).build();

          // Save the updated program
          programRepository.updateProgramSync(updatedProgramDefinition.toProgram());
        });
  }

  private BlockDefinition updateBlockIfEnumerator(
      BlockDefinition blockDefinition, ImmutableList<Long> enumeratorQuestionIds) {
    boolean hasEnumerator =
        blockDefinition.programQuestionDefinitions().stream()
            .map(ProgramQuestionDefinition::id)
            .anyMatch(enumeratorQuestionIds::contains);
    if (hasEnumerator) {
      return blockDefinition.toBuilder().setIsEnumerator(Optional.of(true)).build();
    }
    return blockDefinition;
  }
}
