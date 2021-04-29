package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.Path;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;

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
  public String getProgramTitle() {
    return programDefinition.getLocalizedNameOrDefault(applicantData.preferredLocale());
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
  // TODO(#783): Need to compute Blocks for repeated questions.
  private ImmutableList<Block> getBlocks(boolean onlyIncludeInProgressBlocks) {
    ImmutableList.Builder<Block> blockListBuilder = ImmutableList.builder();

    ImmutableList<BlockDefinition> nonRepeatedBlockDefinitions =
        programDefinition.getNonRepeatedBlockDefinitions();
    for (BlockDefinition blockDefinition : nonRepeatedBlockDefinitions) {
      Block block =
          new Block(
              String.valueOf(blockDefinition.id()),
              blockDefinition,
              applicantData,
              Path.create("applicant"));

      boolean includeAllBlocks = !onlyIncludeInProgressBlocks;
      if (includeAllBlocks
          || !block.isCompleteWithoutErrors()
          || block.wasCompletedInProgram(programDefinition.id())) {
        blockListBuilder.add(block);
      }
    }

    return blockListBuilder.build();
  }
}
