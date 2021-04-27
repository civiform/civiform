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

  /**
   * Get the list of {@link Block}s this applicant should fill out for this program. This list
   * includes any block that is incomplete or has errors (which indicate the applicant needs to make
   * a correction), or any block that was completed while filling out this program form.
   *
   * @return a list of {@link Block}s that were completed by the applicant in this session or still
   *     need to be completed for this program
   */
  @Override
  public ImmutableList<Block> getCurrentBlockList() {
    if (currentBlockList == null) {
      currentBlockList = getBlockList(true);
    }
    return currentBlockList;
  }

  @Override
  public Optional<Block> getBlock(String blockId) {
    return getAllBlocks().stream()
        .filter((block) -> block.getId().equals(blockId))
        .findFirst();
  }

  @Override
  public Optional<Block> getBlockAfter(String blockId) {
    ImmutableList<Block> blocks = getCurrentBlockList();

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
    return getCurrentBlockList().stream()
        .filter(block -> !block.isCompleteWithoutErrors())
        .findFirst();
  }

  @Override
  public boolean preferredLanguageSupported() {
    return programDefinition.getSupportedLocales().contains(applicantData.preferredLocale());
  }

  private ImmutableList<Block> getAllBlocks() {
    return getBlockList(false);
  }

  // TODO(#783): Need to compute Blocks for repeated questions.
  private ImmutableList<Block> getBlockList(boolean onlyCurrentBlockList) {
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

      if (!onlyCurrentBlockList
          || !block.isCompleteWithoutErrors()
          || block.wasCompletedInProgram(programDefinition.id())) {
        blockListBuilder.add(block);
      }
    }

    return blockListBuilder.build();
  }
}
