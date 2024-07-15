package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import services.LocalizedStrings;
import services.Path;
import services.applicant.predicate.JsonPathPredicateGenerator;
import services.applicant.predicate.JsonPathPredicateGeneratorFactory;
import services.applicant.predicate.PredicateEvaluator;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.program.predicate.PredicateDefinition;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionType;

/** Implementation class for ReadOnlyApplicantProgramService interface. */
public class ReadOnlyApplicantProgramServiceImpl implements ReadOnlyApplicantProgramService {

  private static final String NOT_APPLICABLE = "N/A";

  /**
   * Note that even though {@link ApplicantData} is mutable, we can consider it immutable at this
   * point since there is no shared state between requests. In fact, we call {@link
   * ApplicantData#lock()} in the constructor so no changes can occur. This means that we can
   * memoize attributes based on ApplicantData without concern that the data will change.
   */
  private final ApplicantData applicantData;

  private final ProgramDefinition programDefinition;
  private final String baseUrl;
  private final JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory;
  private ImmutableList<Block> allActiveBlockList;
  private ImmutableList<Block> allHiddenBlockList;
  private ImmutableList<Block> currentBlockList;

  public ReadOnlyApplicantProgramServiceImpl(
      JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory,
      ApplicantData applicantData,
      ProgramDefinition programDefinition,
      String baseUrl) {
    this(
        jsonPathPredicateGeneratorFactory,
        applicantData,
        programDefinition,
        baseUrl,
        /* failedUpdates= */ ImmutableMap.of());
  }

  protected ReadOnlyApplicantProgramServiceImpl(
      JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory,
      ApplicantData applicantData,
      ProgramDefinition programDefinition,
      String baseUrl,
      ImmutableMap<Path, String> failedUpdates) {
    this.jsonPathPredicateGeneratorFactory = checkNotNull(jsonPathPredicateGeneratorFactory);
    this.applicantData =
        new ApplicantData(checkNotNull(applicantData).asJsonString(), applicantData.getApplicant());
    this.applicantData.setPreferredLocale(applicantData.preferredLocale());
    this.applicantData.setFailedUpdates(failedUpdates);
    this.applicantData.lock();
    this.programDefinition = checkNotNull(programDefinition);
    this.baseUrl = checkNotNull(baseUrl);
  }

  @Override
  public ApplicantData getApplicantData() {
    return applicantData;
  }

  @Override
  public String getProgramTitle() {
    return programDefinition.localizedName().getOrDefault(applicantData.preferredLocale());
  }

  @Override
  public Long getProgramId() {
    return this.programDefinition.id();
  }

  @Override
  public ProgramType getProgramType() {
    return this.programDefinition.programType();
  }

  @Override
  public LocalizedStrings getCustomConfirmationMessage() {
    return this.programDefinition.localizedConfirmationMessage();
  }

  @Override
  public ImmutableList<String> getStoredFileKeys(boolean multipleUploadsEnabled) {
    return getAllActiveBlocks().stream()
        .filter(Block::isFileUpload)
        .flatMap(block -> block.getQuestions().stream())
        .filter(ApplicantQuestion::isAnswered)
        .filter(ApplicantQuestion::isFileUploadQuestion)
        .map(ApplicantQuestion::createFileUploadQuestion)
        .flatMap(question -> getFileKeyValues(question, multipleUploadsEnabled))
        .collect(ImmutableList.toImmutableList());
  }

  private static Stream<String> getFileKeyValues(
      FileUploadQuestion question, boolean multipleUploadsEnabled) {
    return multipleUploadsEnabled
        ? question.getFileKeyListValue().orElseGet(() -> ImmutableList.<String>of()).stream()
        : question.getFileKeyValue().stream();
  }

  @Override
  public boolean isApplicationEligible() {
    return getAllActiveBlocks().stream().allMatch(block -> isBlockEligible(block));
  }

  @Override
  public boolean isApplicationNotEligible() {
    return getAllActiveBlocks().stream()
        .anyMatch(
            block ->
                block.getQuestions().stream()
                    .anyMatch(
                        question ->
                            !isQuestionEligibleInBlock(block, question) && question.isAnswered()));
  }

  @Override
  public boolean blockHasEligibilityPredicate(String blockId) {
    Block block = getActiveBlock(blockId).get();
    Optional<PredicateDefinition> predicate =
        block.getEligibilityDefinition().map(EligibilityDefinition::predicate);
    return !predicate.isEmpty();
  }

  @Override
  public boolean isActiveBlockEligible(String blockId) {
    Block block = getActiveBlock(blockId).get();
    return isBlockEligible(block);
  }

  /** Helper functions returning if the block eligibility criteria are met. */
  private boolean isBlockEligible(Block block) {
    Optional<PredicateDefinition> predicate =
        block.getEligibilityDefinition().map(EligibilityDefinition::predicate);
    // No eligibility criteria means the block is eligible.
    if (predicate.isEmpty()) {
      return true;
    }
    return evaluatePredicate(block, predicate.get());
  }

  @Override
  public ImmutableList<Block> getAllActiveBlocks() {
    if (allActiveBlockList == null) {
      allActiveBlockList = getBlocks((block) -> showBlock(block));
    }
    return allActiveBlockList;
  }

  @Override
  public ImmutableList<Block> getAllHiddenBlocks() {
    if (allHiddenBlockList == null) {
      allHiddenBlockList = getBlocks((block) -> !showBlock(block));
    }
    return allHiddenBlockList;
  }

  @Override
  public int getActiveAndCompletedInProgramBlockCount() {
    return Math.toIntExact(
        getAllActiveBlocks().stream().filter(Block::isCompletedInProgramWithoutErrors).count());
  }

  @Override
  public ImmutableList<Block> getInProgressBlocks() {
    if (currentBlockList == null) {
      currentBlockList =
          getBlocks(
              block ->
                  // Return all blocks that contain errors, were answered in this program, or
                  // contain a static question.
                  (!block.isAnsweredWithoutErrors()
                          || block.wasAnsweredInProgram(programDefinition.id())
                          || block.containsStatic())
                      && showBlock(block));
    }
    return currentBlockList;
  }

  @Override
  public ImmutableList<ApplicantQuestion> getIneligibleQuestions() {
    ImmutableList<Block> blocks = getAllActiveBlocks();
    List<ApplicantQuestion> questionList = new ArrayList<>();
    for (Block block : blocks) {
      if (block.getEligibilityDefinition().isPresent()) {
        ImmutableList<Long> eligibilityQuestions =
            block.getEligibilityDefinition().get().predicate().getQuestions();
        eligibilityQuestions.forEach(
            question -> {
              try {
                ApplicantQuestion applicantQuestion = block.getQuestion(question.longValue());
                if (applicantQuestion.isAnswered()
                    && !isQuestionEligibleInBlock(block, applicantQuestion)) {
                  questionList.add(applicantQuestion);
                }
              } catch (QuestionNotFoundException e) {
                throw new RuntimeException(e);
              }
            });
      }
    }
    return questionList.stream().distinct().collect(toImmutableList());
  }

  @Override
  public Optional<Block> getHiddenBlock(String blockId) {
    return getAllHiddenBlocks().stream()
        .filter((block) -> block.getId().equals(blockId))
        .findFirst();
  }

  @Override
  public Optional<Block> getActiveBlock(String blockId) {
    return getAllActiveBlocks().stream()
        .filter((block) -> block.getId().equals(blockId))
        .findFirst();
  }

  @Override
  public Optional<Block> getInProgressBlockAfter(String blockId) {
    ImmutableList<Block> blocks = getInProgressBlocks();
    for (int i = 0; i < blocks.size() - 1; i++) {
      if (blocks.get(i).getId().equals(blockId)) {
        return Optional.of(blocks.get(i + 1));
      }
    }
    return Optional.empty();
  }

  @Override
  public int getBlockIndex(String blockId) {
    ImmutableList<Block> allBlocks = getAllActiveBlocks();

    for (int i = 0; i < allBlocks.size(); i++) {
      if (allBlocks.get(i).getId().equals(blockId)) {
        return i;
      }
    }

    return -1;
  }

  /** Returns the first block with an unanswered question or static block. */
  @Override
  public Optional<Block> getFirstIncompleteOrStaticBlock() {
    return getInProgressBlocks().stream()
        .filter(block -> !block.isCompletedInProgramWithoutErrors() || block.containsStatic())
        .findFirst();
  }

  /** Returns the first block with an unanswered question. */
  @Override
  public Optional<Block> getFirstIncompleteBlockExcludingStatic() {
    return getInProgressBlocks().stream()
        .filter(block -> !block.isCompletedInProgramWithoutErrors())
        .findFirst();
  }

  @Override
  public boolean preferredLanguageSupported() {
    return programDefinition.getSupportedLocales().contains(applicantData.preferredLocale());
  }

  @Override
  public ImmutableList<AnswerData> getSummaryDataAllQuestions() {
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> blocks = getBlocks((block) -> true);
    addDataToBuilder(blocks, builder, /* showAnswerText */ true);
    return builder.build();
  }

  @Override
  public ImmutableList<AnswerData> getSummaryDataOnlyActive() {
    // TODO: We need to be able to use this on the admin side with admin-specific l10n.
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> activeBlocks = getAllActiveBlocks();
    addDataToBuilder(activeBlocks, builder, /* showAnswerText= */ true);
    return builder.build();
  }

  @Override
  public ImmutableList<AnswerData> getSummaryDataOnlyHidden() {
    // TODO: We need to be able to use this on the admin side with admin-specific l10n.
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> hiddenBlocks = getAllHiddenBlocks();
    addDataToBuilder(hiddenBlocks, builder, /* showAnswerText= */ false);
    return builder.build();
  }

  /**
   * Helper method for {@link ReadOnlyApplicantProgramServiceImpl#getSummaryDataOnlyActive()} and
   * {@link ReadOnlyApplicantProgramServiceImpl#getSummaryDataOnlyHidden()}. Adds {@link AnswerData}
   * data to {@link ImmutableList.Builder<AnswerData>}.
   *
   * @param blocks the blocks to add to the builder
   * @param builder the builder to add the blocks to
   * @param showAnswerText whether to include the answer text in the result. If {@code false},
   *     answers are replaced with "N/A".
   */
  private void addDataToBuilder(
      ImmutableList<Block> blocks,
      ImmutableList.Builder<AnswerData> builder,
      boolean showAnswerText) {
    for (Block block : blocks) {
      ImmutableList<ApplicantQuestion> questions = block.getQuestions();
      for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
        ApplicantQuestion applicantQuestion = questions.get(questionIndex);
        // Don't include static content in summary data.
        if (applicantQuestion.getType().equals(QuestionType.STATIC)) {
          continue;
        }
        boolean isAnswered = applicantQuestion.isAnswered();
        boolean isEligible = isQuestionEligibleInBlock(block, applicantQuestion);
        String questionText = applicantQuestion.getQuestionText();
        String questionTextForScreenReader = applicantQuestion.getQuestionTextForScreenReader();
        String answerText =
            showAnswerText ? applicantQuestion.getQuestion().getAnswerString() : NOT_APPLICABLE;
        Optional<Long> timestamp = applicantQuestion.getLastUpdatedTimeMetadata();
        Optional<Long> updatedProgram = applicantQuestion.getUpdatedInProgramMetadata();
        Optional<String> originalFileName = Optional.empty();
        Optional<String> encodedFileKey = Optional.empty();
        if (isAnswered && applicantQuestion.isFileUploadQuestion()) {
          FileUploadQuestion fileUploadQuestion = applicantQuestion.createFileUploadQuestion();
          originalFileName = fileUploadQuestion.getOriginalFileName();
          encodedFileKey =
              fileUploadQuestion
                  .getFileKeyValue()
                  .map((fileKey) -> URLEncoder.encode(fileKey, StandardCharsets.UTF_8));
        }
        boolean isPreviousResponse =
            updatedProgram.isPresent() && updatedProgram.get() != programDefinition.id();
        AnswerData data =
            AnswerData.builder()
                .setProgramId(programDefinition.id())
                .setBlockId(block.getId())
                .setContextualizedPath(applicantQuestion.getContextualizedPath())
                .setQuestionDefinition(applicantQuestion.getQuestionDefinition())
                .setApplicantQuestion(applicantQuestion)
                .setRepeatedEntity(block.getRepeatedEntity())
                .setQuestionIndex(questionIndex)
                .setQuestionText(questionText)
                .setQuestionTextForScreenReader(questionTextForScreenReader)
                .setIsAnswered(isAnswered)
                .setIsEligible(isEligible)
                .setEligibilityIsGating(programDefinition.eligibilityIsGating())
                .setAnswerText(answerText)
                .setEncodedFileKey(encodedFileKey)
                .setOriginalFileName(originalFileName)
                .setTimestamp(timestamp.orElse(AnswerData.TIMESTAMP_NOT_SET))
                .setIsPreviousResponse(isPreviousResponse)
                .setScalarAnswersInDefaultLocale(getScalarAnswers(applicantQuestion))
                .build();
        builder.add(data);
      }
    }
  }

  /**
   * Gets {@link Block}s for this program and applicant. If {@code onlyIncludeInProgressBlocks} is
   * true, then only the current blocks will be included in the list. A block is "in progress" if it
   * has yet to be filled out by the applicant, or if it was filled out in the context of this
   * program.
   */
  private ImmutableList<Block> getBlocks(Predicate<Block> includeBlockIfTrue) {
    String emptyBlockIdSuffix = "";
    return getBlocks(
        programDefinition.getNonRepeatedBlockDefinitions(),
        emptyBlockIdSuffix,
        Optional.empty(),
        includeBlockIfTrue);
  }

  /**
   * True if the {@link ApplicantQuestion} is eligible in the given {@link Block}. If the block is
   * not eligible, check if the question is part of that eligibility condition.
   */
  private boolean isQuestionEligibleInBlock(Block block, ApplicantQuestion question) {
    return isBlockEligible(block)
        || !block
            .getEligibilityDefinition()
            .get()
            .predicate()
            .getQuestions()
            .contains(question.getQuestionDefinition().getId());
  }

  /**
   * Recursive helper method for {@link ReadOnlyApplicantProgramServiceImpl#getBlocks(Predicate)}.
   */
  private ImmutableList<Block> getBlocks(
      ImmutableList<BlockDefinition> blockDefinitions,
      String blockIdSuffix,
      Optional<RepeatedEntity> maybeRepeatedEntity,
      Predicate<Block> includeBlockIfTrue) {
    ImmutableList.Builder<Block> blockListBuilder = ImmutableList.builder();

    for (BlockDefinition blockDefinition : blockDefinitions) {
      // Create and maybe include the block for this block definition.
      Block block =
          new Block(
              blockDefinition.id() + blockIdSuffix,
              blockDefinition,
              applicantData,
              maybeRepeatedEntity);
      if (includeBlockIfTrue.test(block)) {
        blockListBuilder.add(block);
      }

      // For an enumeration block definition, build blocks for its repeated questions
      if (blockDefinition.isEnumerator()) {
        // Get all the repeated entities enumerated by this enumerator question.
        EnumeratorQuestionDefinition enumeratorQuestionDefinition =
            blockDefinition.getEnumerationQuestionDefinition();
        ImmutableList<RepeatedEntity> repeatedEntities =
            maybeRepeatedEntity
                .map(
                    e ->
                        e.createNestedRepeatedEntities(
                            enumeratorQuestionDefinition,
                            block.getVisibilityPredicate(),
                            applicantData))
                .orElse(
                    RepeatedEntity.createRepeatedEntities(
                        enumeratorQuestionDefinition,
                        block.getVisibilityPredicate(),
                        applicantData));
        // For each repeated entity, recursively build blocks for all the repeated blocks of this
        // enumerator block.
        ImmutableList<BlockDefinition> repeatedBlockDefinitions =
            programDefinition.getBlockDefinitionsForEnumerator(blockDefinition.id());
        for (int i = 0; i < repeatedEntities.size(); i++) {
          String nextBlockIdSuffix = String.format("%s-%d", blockIdSuffix, i);
          blockListBuilder.addAll(
              getBlocks(
                  repeatedBlockDefinitions,
                  nextBlockIdSuffix,
                  Optional.of(repeatedEntities.get(i)),
                  includeBlockIfTrue));
        }
      }
    }

    return blockListBuilder.build();
  }

  private boolean showBlock(Block block) {
    if (block.getRepeatedEntity().isPresent()) {
      // In repeated blocks, test if this block's parents are visible.
      ImmutableList<PredicateDefinition> nestedVisibility =
          block.getRepeatedEntity().get().nestedVisibility();
      if (nestedVisibility.stream()
          .filter(predicate -> !evaluateVisibility(block, predicate))
          .findAny()
          .isPresent()) {
        return false;
      }
    }
    if (block.getVisibilityPredicate().isEmpty()) {
      // Default to show
      return true;
    }
    return this.evaluateVisibility(block, block.getVisibilityPredicate().get());
  }

  private boolean evaluateVisibility(Block block, PredicateDefinition predicate) {
    boolean evaluation = evaluatePredicate(block, predicate);
    switch (predicate.action()) {
      case HIDE_BLOCK:
        return !evaluation;
      case SHOW_BLOCK:
        return evaluation;
      default:
        return true;
    }
  }

  private boolean evaluatePredicate(Block block, PredicateDefinition predicate) {
    JsonPathPredicateGenerator predicateGenerator =
        jsonPathPredicateGeneratorFactory.create(
            this.programDefinition.streamQuestionDefinitions().collect(toImmutableList()),
            block.getRepeatedEntity());
    return new PredicateEvaluator(this.applicantData, predicateGenerator)
        .evaluate(predicate.rootNode());
  }

  /**
   * Returns the {@link Path}s and their corresponding scalar answers to a {@link
   * ApplicantQuestion}. Answers do not include metadata.
   */
  // TODO(#4872): remove this method.
  private ImmutableMap<Path, String> getScalarAnswers(ApplicantQuestion question) {
    switch (question.getType()) {
      case DROPDOWN:
      case RADIO_BUTTON:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.SELECTION),
            question.createSingleSelectQuestion().getSelectedOptionAdminName().orElse(""));
      case CURRENCY:
        CurrencyQuestion currencyQuestion = question.createCurrencyQuestion();
        return ImmutableMap.of(
            currencyQuestion.getCurrencyPath(), currencyQuestion.getAnswerString());
      case CHECKBOX:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.SELECTIONS),
            question
                .createMultiSelectQuestion()
                .getSelectedOptionAdminNames()
                .map(
                    selectedOptions ->
                        selectedOptions.stream().collect(Collectors.joining(", ", "[", "]")))
                .orElse(""));
      case FILEUPLOAD:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.FILE_KEY),
            question
                .createFileUploadQuestion()
                .getFileKeyValue()
                .map(
                    fileKey ->
                        baseUrl
                            + controllers.routes.FileController.adminShow(
                                    programDefinition.id(),
                                    URLEncoder.encode(fileKey, StandardCharsets.UTF_8))
                                .url())
                .orElse(""));
      case ENUMERATOR:
        return ImmutableMap.of(
            question.getContextualizedPath(),
            question.createEnumeratorQuestion().getAnswerString());
      case DATE:
        DateQuestion dateQuestion = question.createDateQuestion();
        return ImmutableMap.of(dateQuestion.getDatePath(), dateQuestion.getAnswerString());
      default:
        return question.getContextualizedScalars().keySet().stream()
            .filter(path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
            .collect(
                ImmutableMap.toImmutableMap(
                    path -> path, path -> applicantData.readAsString(path).orElse("")));
    }
  }
}
