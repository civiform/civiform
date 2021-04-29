package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.Path;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

public class ReadOnlyApplicantProgramServiceImpl implements ReadOnlyApplicantProgramService {

  private final ApplicantData applicantData;
  private final ProgramDefinition programDefinition;
  private ImmutableList<Block> currentBlockList;

  protected ReadOnlyApplicantProgramServiceImpl(
      ApplicantData applicantData, ProgramDefinition programDefinition) {
    this.applicantData = new ApplicantData(checkNotNull(applicantData).asJsonString());
    this.applicantData.setPreferredLocale(applicantData.preferredLocale());
    this.applicantData.lock();
    this.programDefinition = checkNotNull(programDefinition);
  }

  @Override
  public ImmutableList<Block> getAllBlocks() {
    return getBlocks(false);
  }

  @Override
  public ImmutableList<Block> getInProgressBlocks() {
    if (currentBlockList == null) {
      currentBlockList = getBlocks(true);
    }
    return currentBlockList;
  }

  @Override
  public Optional<Block> getBlock(String blockId) {
    return getAllBlocks().stream().filter((block) -> block.getId().equals(blockId)).findFirst();
  }

  @Override
  public Optional<Block> getBlockAfter(String blockId) {
    ImmutableList<Block> blocks = getInProgressBlocks();

    for (int i = 0; i < blocks.size() - 1; i++) {
      if (!blocks.get(i).getId().equals(blockId)) {
        continue;
      }

      return Optional.of(blocks.get(i + 1));
    }

    return Optional.empty();
  }

  @Override
  public Optional<Block> getBlockAfter(Block block) {
    return getBlockAfter(block.getId());
  }

  @Override
  public Optional<Block> getFirstIncompleteBlock() {
    return getInProgressBlocks().stream()
        .filter(block -> !block.isCompleteWithoutErrors())
        .findFirst();
  }

  @Override
  public boolean preferredLanguageSupported() {
    return programDefinition.getSupportedLocales().contains(applicantData.preferredLocale());
  }

  /**
   * Gets {@link Block}s for this program and applicant. If {@code onlyIncludeInProgressBlocks} is
   * true, then only the current blocks will be included in the list. A block is "in progress" if it
   * has yet to be filled out by the applicant, or if it was filled out in the context of this
   * program.
   */
  private ImmutableList<Block> getBlocks(boolean onlyIncludeInProgressBlocks) {
    String emptyBlockIdSuffix = "";
    return getBlocks(
        programDefinition.getNonRepeatedBlockDefinitions(),
        emptyBlockIdSuffix,
        ApplicantData.APPLICANT_PATH,
        onlyIncludeInProgressBlocks);
  }

  /** Recursive helper method for {@link ReadOnlyApplicantProgramServiceImpl#getBlocks(boolean)}. */
  private ImmutableList<Block> getBlocks(
      ImmutableList<BlockDefinition> blockDefinitions,
      String blockIdSuffix,
      Path contextualizedPath,
      boolean onlyIncludeInProgressBlocks) {
    ImmutableList.Builder<Block> blockListBuilder = ImmutableList.builder();

    for (BlockDefinition blockDefinition : blockDefinitions) {
      // Create and maybe include the block for this block definition.
      Block block =
          new Block(
              blockDefinition.id() + blockIdSuffix,
              blockDefinition,
              applicantData,
              contextualizedPath);
      boolean includeAllBlocks = !onlyIncludeInProgressBlocks;
      if (includeAllBlocks
          || !block.isCompleteWithoutErrors()
          || block.wasCompletedInProgram(programDefinition.id())) {
        blockListBuilder.add(block);
      }

      // For a repeater block definition, recursively build blocks for its repeated questions
      if (blockDefinition.isRepeater()) {
        QuestionDefinition enumerationQuestionDefinition = blockDefinition.getQuestionDefinition(0);

        // Update the contextualized path by joining the repeater question's path segment
        Path repeatedContextualizedPath =
            contextualizedPath.join(enumerationQuestionDefinition.getQuestionPathSegment());

        // For each repeated entity, recursively build blocks for all of the repeated blocks of this
        // repeater block.
        ImmutableList<String> entityNames =
            applicantData.readRepeatedEntities(repeatedContextualizedPath);
        ImmutableList<BlockDefinition> nextBlockDefinitions =
            programDefinition.getBlockDefinitionsForRepeater(blockDefinition.id());
        for (int i = 0; i < entityNames.size(); i++) {
          String nextIdSuffix = String.format("%s-%d", blockIdSuffix, i);
          Path nextContextualizedPath = repeatedContextualizedPath.atIndex(i);
          blockListBuilder.addAll(
              getBlocks(
                  nextBlockDefinitions,
                  nextIdSuffix,
                  nextContextualizedPath,
                  onlyIncludeInProgressBlocks));
        }
      }
    }

    return blockListBuilder.build();
  }
}
