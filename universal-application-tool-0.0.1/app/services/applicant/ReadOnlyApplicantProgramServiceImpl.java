package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.program.ProgramDefinition;
import services.question.ReadOnlyQuestionService;

public class ReadOnlyApplicantProgramServiceImpl implements ReadOnlyApplicantProgramService {

  private final ApplicantData applicantData;
  private final ProgramDefinition programDefinition;
  private final ReadOnlyQuestionService readOnlyQuestionService;
  private Optional<ImmutableList<Block>> currentBlockList = Optional.empty();

  protected ReadOnlyApplicantProgramServiceImpl(ApplicantData applicantData,
      ProgramDefinition programDefinition, ReadOnlyQuestionService readOnlyQuestionService) {
    this.applicantData = checkNotNull(applicantData);
    this.programDefinition = checkNotNull(programDefinition);
    this.readOnlyQuestionService = checkNotNull(readOnlyQuestionService);
  }

  @Override
  public ImmutableList<Block> getCurrentBlockList() {
    if (currentBlockList.isPresent()) {
      return currentBlockList.get();
    }

    ImmutableList<Block> blocks = programDefinition.blockDefinitions().stream()
        .map(blockDefinition -> new Block(
            blockDefinition.id(),
            blockDefinition,
            applicantData
        )).collect(toImmutableList());

    currentBlockList = Optional.of(blocks);

    return blocks;
  }

  @Override
  public Optional<Block> getBlock(long blockId) {
    return getCurrentBlockList().stream().filter((block) -> block.getId() == blockId).findFirst();
  }

  @Override
  public Optional<Block> getBlockAfter(long blockId) {
    ImmutableList<Block> blocks = getCurrentBlockList();

    for (int i = 0; i < blocks.size() - 1; i++) {
      if (blocks.get(i).getId() != blockId) {
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
    return Optional.of(getCurrentBlockList().get(0));
  }
}
