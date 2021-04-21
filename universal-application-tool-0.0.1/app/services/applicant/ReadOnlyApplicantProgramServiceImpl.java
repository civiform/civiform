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
    if (currentBlockList.isPresent()) {
      return currentBlockList.get();
    }

    // TODO(#783): Instead of just streaming blockDefinitions, need to incorporate repeated blocks
    //  and their indices.

    // TODO(#783): The following snippet does not account for recursive repeater blocks.
    //

    // Something to kickstart the recursive method with all non-repeated blocks (AKA top-level blocks)

    // Recursive method:
    //   List builder
    //   Add the current block
    //   Add all (recursive method)

    // for each blockDef:
    //   if NOT blockDef.isRepeated:  (we account for repeated blocks below)
    //     construct a block
    //   if blockDef.isRepeater:
    //     for each item ("i") in repeater (e.g., each household member):
    //       for each consecutive following "repeated" block ("j") (e.g., hm name, hm address):
    //         construct a block with RepeaterContext "i,j"
    // REFER TO ProgramBlockEditView#renderBlockList
    // Recursively build up the RepeaterContext

    ImmutableList<Block> blocks =
        programDefinition.blockDefinitions().stream()
            // TODO(#783): Pass in repeaterContext to Block constructor.
            //  And/or create ID that fully represents the Block
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

  @Override
  public boolean preferredLanguageSupported() {
    return programDefinition.getSupportedLocales().contains(applicantData.preferredLocale());
  }

  // TODO(#783): Need to compute Blocks different for repeaters and repeateds. See getCurrentBlockList comments.
  private ImmutableList<Block> getAllBlocksForThisProgram() {
    return programDefinition.blockDefinitions().stream()
        .map(blockDefinition -> new Block(blockDefinition.id(), blockDefinition, applicantData))
        .collect(toImmutableList());
  }
}
