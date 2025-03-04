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
import java.util.stream.Stream;
import models.ApplicantModel;
import services.LocalizedStrings;
import services.Path;
import services.applicant.predicate.JsonPathPredicateGenerator;
import services.applicant.predicate.JsonPathPredicateGeneratorFactory;
import services.applicant.predicate.PredicateEvaluator;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.program.predicate.PredicateDefinition;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionType;

/** Provides synchronous, read-only behavior relevant to an applicant for a specific program. */
public final class ReadOnlyApplicantProgramService {

  private static final String NOT_APPLICABLE = "N/A";

  /**
   * Note that even though {@link ApplicantData} is mutable, we can consider it immutable at this
   * point since there is no shared state between requests. In fact, we call {@link
   * ApplicantData#lock()} in the constructor so no changes can occur. This means that we can
   * memoize attributes based on ApplicantData without concern that the data will change.
   */
  private final ApplicantData applicantData;

  private final ApplicantModel applicant;

  private final ProgramDefinition programDefinition;
  private final JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory;
  private ImmutableList<Block> allActiveBlockList;
  private ImmutableList<Block> allHiddenBlockList;
  private ImmutableList<Block> currentBlockList;

  public ReadOnlyApplicantProgramService(
      JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory,
      ApplicantModel applicant,
      ApplicantData applicantData,
      ProgramDefinition programDefinition) {
    this(
        jsonPathPredicateGeneratorFactory,
        applicant,
        applicantData,
        programDefinition,
        /* failedUpdates= */ ImmutableMap.of());
  }

  public ReadOnlyApplicantProgramService(
      JsonPathPredicateGeneratorFactory jsonPathPredicateGeneratorFactory,
      ApplicantModel applicant,
      ApplicantData applicantData,
      ProgramDefinition programDefinition,
      ImmutableMap<Path, String> failedUpdates) {
    this.jsonPathPredicateGeneratorFactory = checkNotNull(jsonPathPredicateGeneratorFactory);
    this.applicant = checkNotNull(applicant);
    this.applicantData = new ApplicantData(checkNotNull(applicantData).asJsonString());
    this.applicantData.setPreferredLocale(applicantData.preferredLocale());
    this.applicantData.setFailedUpdates(failedUpdates);
    this.applicantData.lock();
    this.programDefinition = checkNotNull(programDefinition);
  }

  /** Returns the applicant model for this application. */
  public ApplicantModel getApplicant() {
    return applicant;
  }

  /** Returns the locked applicant data for this application. */
  public ApplicantData getApplicantData() {
    return applicantData;
  }

  /** Returns the program title, localized to the applicant's preferred locale. */
  public String getProgramTitle() {
    return programDefinition.localizedName().getOrDefault(applicantData.preferredLocale());
  }

  /** Returns the program description, localized to the applicant's preferred locale. */
  public String getProgramDescription() {
    return programDefinition.localizedDescription().getOrDefault(applicantData.preferredLocale());
  }

  /** Returns the program short description, localized to the applicant's preferred locale. */
  public String getProgramShortDescription() {
    return programDefinition
        .localizedShortDescription()
        .getOrDefault(applicantData.preferredLocale());
  }

  /** Returns the ID of the program. */
  public Long getProgramId() {
    return this.programDefinition.id();
  }

  /** Returns the ProgramType of the program. */
  public ProgramType getProgramType() {
    return this.programDefinition.programType();
  }

  /**
   * Returns a custom message for the confirmation screen that renders after an applicant submits an
   * application. If a custom message is not set, returns an empty string.
   */
  public LocalizedStrings getCustomConfirmationMessage() {
    return this.programDefinition.localizedConfirmationMessage();
  }

  /** Get the string identifiers for all stored files for this application. */
  public ImmutableList<String> getStoredFileKeys() {
    return getAllActiveBlocks().stream()
        .filter(Block::isFileUpload)
        .flatMap(block -> block.getQuestions().stream())
        .filter(ApplicantQuestion::isAnswered)
        .filter(ApplicantQuestion::isFileUploadQuestion)
        .map(ApplicantQuestion::createFileUploadQuestion)
        .flatMap(ReadOnlyApplicantProgramService::getFileKeyValues)
        .collect(ImmutableList.toImmutableList());
  }

  private static Stream<String> getFileKeyValues(FileUploadQuestion question) {
    return question.getFileKeyListValue().orElseGet(ImmutableList::of).stream();
  }

  /**
   * Returns if all Program eligibility criteria are met. This will return false in some cases where
   * the eligibility questions haven't yet been answered.
   */
  public boolean isApplicationEligible() {
    return getAllActiveBlocks().stream().allMatch(block -> isBlockEligible(block));
  }

  /**
   * True if any of the answered questions in the program are not eligible, even if the application
   * hasn't yet been completed.
   */
  public boolean isApplicationNotEligible() {
    return getAllActiveBlocks().stream()
        .anyMatch(
            block ->
                block.getQuestions().stream()
                    .anyMatch(
                        question ->
                            !isQuestionEligibleInBlock(block, question) && question.isAnswered()));
  }

  /** Returns if the block has an eligibility predicate. */
  public boolean blockHasEligibilityPredicate(String blockId) {
    Block block = getActiveBlock(blockId).get();
    Optional<PredicateDefinition> predicate =
        block.getEligibilityDefinition().map(EligibilityDefinition::predicate);
    return !predicate.isEmpty();
  }

  /** Returns if the active block eligibility criteria are met. */
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

  /**
   * Get a list of all {@link ApplicantQuestion}s in the program.
   *
   * @return A stream of the questions in the program.
   */
  public Stream<ApplicantQuestion> getAllQuestions() {
    return getBlocks((block) -> true).stream().flatMap((block) -> block.getQuestions().stream());
  }

  /**
   * Get the {@link Block}s for this program and applicant. This includes all blocks an applicant
   * must complete for this program, regardless of whether the block was filled out in this program
   * or a previous program. This will not include blocks that are hidden from the applicant (i.e.
   * they have a show/hide predicate).
   */
  public ImmutableList<Block> getAllActiveBlocks() {
    if (allActiveBlockList == null) {
      allActiveBlockList = getBlocks((block) -> showBlock(block));
    }
    return allActiveBlockList;
  }

  /**
   * Get the {@link Block}s for this program and applicant. This only includes blocks that are
   * hidden from the applicant (i.e.they have a show/hide predicate).
   */
  public ImmutableList<Block> getAllHiddenBlocks() {
    if (allHiddenBlockList == null) {
      allHiddenBlockList = getBlocks((block) -> !showBlock(block));
    }
    return allHiddenBlockList;
  }

  /**
   * Get the count of blocks in this program that the applicant should see which have all their
   * questions answered or optional questions skipped.
   *
   * @return the count of active blocks completed in this program.
   */
  public int getActiveAndCompletedInProgramBlockCount() {
    return Math.toIntExact(
        getAllActiveBlocks().stream().filter(Block::isCompletedInProgramWithoutErrors).count());
  }

  /**
   * Get the {@link Block}s this applicant needs to fill out or has filled out for this program.
   *
   * <p>This list includes any block that is incomplete or has errors (which indicate the applicant
   * needs to make a correction), or any block that was completed while filling out this program
   * form. If a block has a show/hide predicate that depends on a question that has not been
   * answered yet (i.e. we cannot determine whether the predicate is true or false), it is included
   * in this list.
   *
   * <p>This list does not include blocks that were completely filled out in a different program.
   *
   * @return a list of {@link Block}s that were completed by the applicant in this session or still
   *     need to be completed for this program
   */
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

  /**
   * Returns whether the applicant should see the eligibility message. This is based on whether the
   * applicant has answered any eligibility questions in the program AND whether eligibility is
   * gating or the application is eligible. When non-gating eligibility is enabled and the person
   * doesn't qualify, we don't show them a message.
   */
  public boolean shouldDisplayEligibilityMessage() {
    return hasAnsweredEligibilityQuestions() && hasGatingEligibilityEnabledOrEligible();
  }

  /** Returns whether eligibility is gating or the application is eligible. */
  private boolean hasGatingEligibilityEnabledOrEligible() {
    if (programDefinition.eligibilityIsGating()) {
      return true;
    }
    return isApplicationEligible();
  }

  /** Returns whether the applicant has answered any eligibility questions in the program. */
  private boolean hasAnsweredEligibilityQuestions() {
    return getAllActiveBlocks().stream()
        .filter(b -> b.answeredQuestionsCount() > 0)
        .anyMatch(
            block -> {
              if (block.getEligibilityDefinition().isPresent()) {
                return block.getEligibilityDefinition().get().predicate().getQuestions().stream()
                    .anyMatch(
                        question -> {
                          try {
                            return block.getQuestion(question.longValue()).isAnswered();
                          } catch (QuestionNotFoundException e) {
                            throw new RuntimeException(e);
                          }
                        });
              }
              return false;
            });
  }

  /**
   * Get a list of questions that the applicant is currently not eligible for based on their answers
   * from active blocks in the program.
   */
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

  /** Get the hidden block with the given block ID if there is one. It is empty if there isn't. */
  public Optional<Block> getHiddenBlock(String blockId) {
    return getAllHiddenBlocks().stream()
        .filter((block) -> block.getId().equals(blockId))
        .findFirst();
  }

  /**
   * Get the active block with the given block ID if there is one. It is empty if there isn't.
   * Active block is the block an applicant must complete for this program. This will not include
   * blocks that are hidden from the applicant.
   */
  public Optional<Block> getActiveBlock(String blockId) {
    return getAllActiveBlocks().stream()
        .filter((block) -> block.getId().equals(blockId))
        .findFirst();
  }

  /**
   * Get the next in-progress block that comes after the block with the given ID if there is one.
   */
  public Optional<Block> getInProgressBlockAfter(String blockId) {
    ImmutableList<Block> blocks = getInProgressBlocks();
    for (int i = 0; i < blocks.size() - 1; i++) {
      if (blocks.get(i).getId().equals(blockId)) {
        return Optional.of(blocks.get(i + 1));
      }
    }
    return Optional.empty();
  }

  /** Returns the index of the given block in the context of all blocks of the program. */
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
  public Optional<Block> getFirstIncompleteOrStaticBlock() {
    return getInProgressBlocks().stream()
        .filter(block -> !block.isCompletedInProgramWithoutErrors() || block.containsStatic())
        .findFirst();
  }

  /** Returns the first block with an unanswered question. */
  public Optional<Block> getFirstIncompleteBlockExcludingStatic() {
    return getInProgressBlocks().stream()
        .filter(block -> !block.isCompletedInProgramWithoutErrors())
        .findFirst();
  }

  /**
   * Returns true if this program fully supports this applicant's preferred language, and false
   * otherwise.
   */
  public boolean preferredLanguageSupported() {
    return programDefinition.getSupportedLocales().contains(applicantData.preferredLocale());
  }

  /**
   * Returns summary data for each question in this application. Includes blocks that are hidden
   * from the applicant due to visibility conditions.
   */
  public ImmutableList<AnswerData> getSummaryDataAllQuestions() {
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> blocks = getBlocks((block) -> true);
    addDataToBuilder(blocks, builder, /* showAnswerText */ true);
    return builder.build();
  }

  /**
   * Returns summary data for each question in the active blocks in this application. Active block
   * is the block an applicant must complete for this program. This will not include blocks that are
   * hidden from the applicant.
   */
  public ImmutableList<AnswerData> getSummaryDataOnlyActive() {
    // TODO: We need to be able to use this on the admin side with admin-specific l10n.
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> activeBlocks = getAllActiveBlocks();
    addDataToBuilder(activeBlocks, builder, /* showAnswerText= */ true);
    return builder.build();
  }

  /**
   * Returns summary data for each question in the hidden blocks in this application. Hidden block
   * is the block not visible to the applicant based on the visibility setting by the admin. This
   * will not include blocks that are active.
   */
  public ImmutableList<AnswerData> getSummaryDataOnlyHidden() {
    // TODO: We need to be able to use this on the admin side with admin-specific l10n.
    ImmutableList.Builder<AnswerData> builder = new ImmutableList.Builder<>();
    ImmutableList<Block> hiddenBlocks = getAllHiddenBlocks();
    addDataToBuilder(hiddenBlocks, builder, /* showAnswerText= */ false);
    return builder.build();
  }

  /**
   * Helper method for {@link ReadOnlyApplicantProgramService#getSummaryDataOnlyActive()} and {@link
   * ReadOnlyApplicantProgramService#getSummaryDataOnlyHidden()}. Adds {@link AnswerData} data to
   * {@link ImmutableList.Builder<AnswerData>}.
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
        ImmutableList<String> encodedFileKeys = ImmutableList.of();
        ImmutableList<String> fileNames = ImmutableList.of();

        if (isAnswered && applicantQuestion.isFileUploadQuestion()) {
          FileUploadQuestion fileUploadQuestion = applicantQuestion.createFileUploadQuestion();
          originalFileName = fileUploadQuestion.getOriginalFileName();
          encodedFileKey =
              fileUploadQuestion
                  .getFileKeyValue()
                  .map((fileKey) -> URLEncoder.encode(fileKey, StandardCharsets.UTF_8));

          if (fileUploadQuestion.getFileKeyListValue().isPresent()) {
            ImmutableList<String> fileKeys = fileUploadQuestion.getFileKeyListValue().get();
            Optional<ImmutableList<String>> originalFileNamesOptional =
                fileUploadQuestion.getOriginalFileNameListValue();
            ImmutableList<String> storageFileNames;
            // Only Azure deployments store OriginalFilenames, for all other deployments, the
            // filename is stored in the
            // filekey.
            storageFileNames =
                originalFileNamesOptional.isPresent() ? originalFileNamesOptional.get() : fileKeys;
            fileNames =
                storageFileNames.stream()
                    .map(FileUploadQuestion::getFileName)
                    .collect(toImmutableList());
            encodedFileKeys =
                fileKeys.stream()
                    .map((fileKey) -> URLEncoder.encode(fileKey, StandardCharsets.UTF_8))
                    .collect(toImmutableList());
          }
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
                .setEncodedFileKeys(encodedFileKeys)
                .setFileNames(fileNames)
                .setOriginalFileName(originalFileName)
                .setTimestamp(timestamp.orElse(AnswerData.TIMESTAMP_NOT_SET))
                .setIsPreviousResponse(isPreviousResponse)
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

  /** Recursive helper method for {@link ReadOnlyApplicantProgramService#getBlocks(Predicate)}. */
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
              applicant,
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
}
