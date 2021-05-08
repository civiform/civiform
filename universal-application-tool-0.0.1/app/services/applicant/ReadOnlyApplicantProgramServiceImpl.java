package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import services.LocalizedStrings;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.LocalizedQuestionOption;
import services.question.types.QuestionDefinition;

public class ReadOnlyApplicantProgramServiceImpl implements ReadOnlyApplicantProgramService {

  private final ApplicantData applicantData;
  private final ProgramDefinition programDefinition;
  private ImmutableList<Block> allBlockList;
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
    return programDefinition.localizedName().getOrDefault(applicantData.preferredLocale());
  }

  @Override
  public ImmutableList<Block> getAllBlocks() {
    if (allBlockList == null) {
      allBlockList = getBlocks(false);
    }
    return allBlockList;
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
  public int getCompletionPercent(String blockId) {
    ImmutableList<Block> blocks = getAllBlocks();

    double blockCount = blocks.size();
    // If there aren't any blocks then I guess we're done.
    if (blockCount == 0) {
      return 100;
    }

    double blockIndex = -1;
    for (int i = 0; i < blocks.size() && blockIndex == -1; i++) {
      if (blocks.get(i).getId().equals(blockId)) {
        blockIndex = i;
      }
    }

    // If the block doesn't exist then return 0.
    if (blockIndex == -1) {
      return 0;
    }

    // If we're on the first block, move the needle a little.
    if (blockIndex == 0) {
      blockIndex = 0.1;
    }

    return (int) ((blockIndex / blockCount) * 100.0);
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
        ImmutableList.of(),
        onlyIncludeInProgressBlocks);
  }

  /** Recursive helper method for {@link ReadOnlyApplicantProgramServiceImpl#getBlocks(boolean)}. */
  private ImmutableList<Block> getBlocks(
      ImmutableList<BlockDefinition> blockDefinitions,
      String blockIdSuffix,
      Path contextualizedPath,
      ImmutableList<RepeatedEntity> repeatedEntities,
      boolean onlyIncludeInProgressBlocks) {
    ImmutableList.Builder<Block> blockListBuilder = ImmutableList.builder();

    for (BlockDefinition blockDefinition : blockDefinitions) {
      // Create and maybe include the block for this block definition.
      Block block =
          new Block(
              blockDefinition.id() + blockIdSuffix,
              blockDefinition,
              applicantData,
              contextualizedPath,
              repeatedEntities);
      boolean includeAllBlocks = !onlyIncludeInProgressBlocks;
      if (includeAllBlocks
          || !block.isCompleteWithoutErrors()
          || block.wasCompletedInProgram(programDefinition.id())) {
        blockListBuilder.add(block);
      }

      // For an enumeration block definition, build blocks for its repeated questions
      if (blockDefinition.isEnumerator()) {

        // Get all the repeated entities enumerated by this enumeration block.
        QuestionDefinition enumerationQuestionDefinition =
            blockDefinition.getEnumerationQuestionDefinition();
        Path contextualizedPathForEnumeration =
            contextualizedPath.join(enumerationQuestionDefinition.getQuestionPathSegment());
        ImmutableList<String> repeatedEntityNames =
            applicantData.readRepeatedEntities(contextualizedPathForEnumeration);

        // For each repeated entity, recursively build blocks for all of the repeated blocks of this
        // enumeration block.
        ImmutableList<BlockDefinition> repeatedBlockDefinitions =
            programDefinition.getBlockDefinitionsForEnumerator(blockDefinition.id());
        for (int i = 0; i < repeatedEntityNames.size(); i++) {
          ImmutableList<RepeatedEntity> updatedRepeatedEntities =
              ImmutableList.<RepeatedEntity>builder()
                  .addAll(repeatedEntities)
                  .add(
                      RepeatedEntity.create(
                          enumerationQuestionDefinition, repeatedEntityNames.get(i)))
                  .build();
          String nextBlockIdSuffix = String.format("%s-%d", blockIdSuffix, i);
          Path contextualizedPathForEntity = contextualizedPathForEnumeration.atIndex(i);
          blockListBuilder.addAll(
              getBlocks(
                  repeatedBlockDefinitions,
                  nextBlockIdSuffix,
                  contextualizedPathForEntity,
                  updatedRepeatedEntities,
                  onlyIncludeInProgressBlocks));
        }
      }
    }

    return blockListBuilder.build();
  }

  @Override
  public ImmutableList<AnswerData> getSummaryData() {
    // TODO: We need to be able to use this on the admin side with admin-specific l10n.
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> blocks = getAllBlocks();
    for (Block block : blocks) {
      for (ApplicantQuestion question : block.getQuestions()) {
        String questionText = question.getQuestionText();
        String answerText = question.errorsPresenter().getAnswerString();
        Optional<Long> timestamp = question.getLastUpdatedTimeMetadata();
        Optional<Long> updatedProgram = question.getUpdatedInProgramMetadata();
        boolean isPreviousResponse =
            updatedProgram.isPresent() && updatedProgram.get() != programDefinition.id();
        AnswerData data =
            AnswerData.builder()
                .setProgramId(programDefinition.id())
                .setBlockId(block.getId())
                .setQuestionId(question.getQuestionDefinition().getId())
                .setQuestionText(questionText)
                .setAnswerText(answerText)
                .setTimestamp(timestamp.orElse(AnswerData.TIMESTAMP_NOT_SET))
                .setIsPreviousResponse(isPreviousResponse)
                .setScalarAnswersInDefaultLocale(
                    getScalarAnswers(question, LocalizedStrings.DEFAULT_LOCALE))
                .build();
        builder.add(data);
      }
    }
    return builder.build();
  }

  /**
   * Returns the {@link Path}s and their corresponding scalar answers to a {@link
   * ApplicantQuestion}. Answers do not include metadata.
   */
  private ImmutableMap<Path, String> getScalarAnswers(ApplicantQuestion question, Locale locale) {
    switch (question.getType()) {
      case DROPDOWN:
      case RADIO_BUTTON:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.SELECTION),
            question
                .createSingleSelectQuestion()
                .getSelectedOptionValue(locale)
                .map(LocalizedQuestionOption::optionText)
                .orElse(""));
      case CHECKBOX:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.SELECTION),
            question
                .createMultiSelectQuestion()
                .getSelectedOptionsValue(locale)
                .map(
                    selectedOptions ->
                        selectedOptions.stream()
                            .map(LocalizedQuestionOption::optionText)
                            .collect(Collectors.joining(", ")))
                .orElse(""));
      case ENUMERATOR:
        return ImmutableMap.of(
            question.getContextualizedPath(),
            question.createEnumeratorQuestion().getAnswerString());
      default:
        return question.getContextualizedScalars().keySet().stream()
            .filter(path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
            .collect(
                ImmutableMap.toImmutableMap(
                    path -> path, path -> applicantData.readAsString(path).orElse("")));
    }
  }
}
