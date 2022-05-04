package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import services.LocalizedStrings;
import services.Path;
import services.applicant.predicate.JsonPathPredicateGenerator;
import services.applicant.predicate.PredicateEvaluator;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.LocalizedQuestionOption;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionType;

/** Implementation class for ReadOnlyApplicantProgramService interface. */
public class ReadOnlyApplicantProgramServiceImpl implements ReadOnlyApplicantProgramService {

  /**
   * Note that even though {@link ApplicantData} is mutable, we can consider it immutable at this
   * point since there is no shared state between requests. In fact, we call {@link
   * ApplicantData#lock()} in the constructor so no changes can occur. This means that we can
   * memoize attributes based on ApplicantData without concern that the data will change.
   */
  private final ApplicantData applicantData;

  private final ProgramDefinition programDefinition;
  private final String baseUrl;
  private ImmutableList<Block> allBlockList;
  private ImmutableList<Block> currentBlockList;

  protected ReadOnlyApplicantProgramServiceImpl(
      ApplicantData applicantData, ProgramDefinition programDefinition, String baseUrl) {
    this.applicantData = new ApplicantData(checkNotNull(applicantData).asJsonString());
    this.applicantData.setPreferredLocale(applicantData.preferredLocale());
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
  public ImmutableList<Block> getAllActiveBlocks() {
    if (allBlockList == null) {
      allBlockList = getBlocks(this::showBlock);
    }
    return allBlockList;
  }

  @Override
  public int getActiveAndCompletedInProgramBlockCount() {
    return getAllActiveBlocks().stream()
        .filter(block -> block.isCompletedInProgramWithoutErrors())
        .mapToInt(b -> 1)
        .sum();
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
  public Optional<Block> getBlock(String blockId) {
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

  @Override
  public Optional<Block> getFirstIncompleteBlock() {
    return getInProgressBlocks().stream()
        .filter(block -> !block.isCompletedInProgramWithoutErrors() || block.containsStatic())
        .findFirst();
  }

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
  public ImmutableList<AnswerData> getSummaryData() {
    // TODO: We need to be able to use this on the admin side with admin-specific l10n.
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> blocks = getAllActiveBlocks();
    for (Block block : blocks) {
      ImmutableList<ApplicantQuestion> questions = block.getQuestions();
      for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
        ApplicantQuestion question = questions.get(questionIndex);
        // Don't include static content in summary data.
        if (question.getType().equals(QuestionType.STATIC)) {
          continue;
        }
        boolean isAnswered = question.isAnswered();
        String questionText = question.getQuestionText();
        String answerText = question.errorsPresenter().getAnswerString();
        Optional<Long> timestamp = question.getLastUpdatedTimeMetadata();
        Optional<Long> updatedProgram = question.getUpdatedInProgramMetadata();
        Optional<String> originalFileName = Optional.empty();
        Optional<String> fileKey = Optional.empty();
        if (isAnswered && question.getType().equals(QuestionType.FILEUPLOAD)) {
          FileUploadQuestion fileUploadQuestion = question.createFileUploadQuestion();
          originalFileName = fileUploadQuestion.getOriginalFileName();
          fileKey = fileUploadQuestion.getFileKeyValue();
        }
        boolean isPreviousResponse =
            updatedProgram.isPresent() && updatedProgram.get() != programDefinition.id();
        AnswerData data =
            AnswerData.builder()
                .setProgramId(programDefinition.id())
                .setBlockId(block.getId())
                .setContextualizedPath(question.getContextualizedPath())
                .setQuestionDefinition(question.getQuestionDefinition())
                .setApplicantQuestion(question)
                .setRepeatedEntity(block.getRepeatedEntity())
                .setQuestionIndex(questionIndex)
                .setQuestionText(questionText)
                .setIsAnswered(isAnswered)
                .setAnswerText(answerText)
                .setFileKey(fileKey)
                .setOriginalFileName(originalFileName)
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
        // For each repeated entity, recursively build blocks for all of the repeated blocks of this
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
      ImmutableList<PredicateDefinition> nestedVisibility =
          block.getRepeatedEntity().get().nestedVisibility();
      for (int i = 0; i < nestedVisibility.size(); i++) {
        if (!this.evaluateVisibility(block, nestedVisibility.get(i))) {
          return false;
        }
      }
    }
    if (block.getVisibilityPredicate().isEmpty()) {
      // Default to show
      return true;
    }
    return this.evaluateVisibility(block, block.getVisibilityPredicate().get());
  }

  private boolean evaluateVisibility(Block block, PredicateDefinition predicate) {
    JsonPathPredicateGenerator predicateGenerator =
        new JsonPathPredicateGenerator(
            this.programDefinition.streamQuestionDefinitions().collect(toImmutableList()),
            block.getRepeatedEntity());
    PredicateEvaluator predicateEvaluator =
        new PredicateEvaluator(this.applicantData, predicateGenerator);

    switch (predicate.action()) {
      case HIDE_BLOCK:
        return !predicateEvaluator.evaluate(predicate.rootNode());
      case SHOW_BLOCK:
        return predicateEvaluator.evaluate(predicate.rootNode());
      default:
        return true;
    }
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
      case CURRENCY:
        CurrencyQuestion currencyQuestion = question.createCurrencyQuestion();
        return ImmutableMap.of(
            currencyQuestion.getCurrencyPath(), currencyQuestion.getAnswerString());
      case CHECKBOX:
        return ImmutableMap.of(
            question.getContextualizedPath().join(Scalar.SELECTIONS),
            question
                .createMultiSelectQuestion()
                .getSelectedOptionsValue(locale)
                .map(
                    selectedOptions ->
                        selectedOptions.stream()
                            .map(LocalizedQuestionOption::optionText)
                            .collect(Collectors.joining(", ", "[", "]")))
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
