package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.program.ProgramDefinition;

public class ReadOnlyApplicantProgramServiceImpl implements ReadOnlyApplicantProgramService {

  private final ApplicantData applicantData;
  private final ProgramDefinition programDefinition;
  private Optional<ImmutableList<Block>> currentBlockList = Optional.empty();

  protected ReadOnlyApplicantProgramServiceImpl(
      ApplicantData applicantData, ProgramDefinition programDefinition) {
    this.applicantData = checkNotNull(applicantData);
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
    if (currentBlockList.isPresent()) {
      return currentBlockList.get();
    }

    ImmutableList<Block> blocks =
        programDefinition.blockDefinitions().stream()
            .map(blockDefinition -> new Block(blockDefinition.id(), blockDefinition, applicantData))
            .filter(
                block ->
                    !block.isCompleteWithoutErrors()
                        || block.wasCompletedInProgram(programDefinition.id()))
            .collect(toImmutableList());

    currentBlockList = Optional.of(blocks);

    return blocks;
  }

  @Override
  public Optional<Block> getBlock(String blockId) {
    return getAllBlocksForThisProgram().stream()
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

  private ImmutableList<Block> getAllBlocksForThisProgram() {
    return programDefinition.blockDefinitions().stream()
        .map(blockDefinition -> new Block(blockDefinition.id(), blockDefinition, applicantData))
        .collect(toImmutableList());
  }
}
