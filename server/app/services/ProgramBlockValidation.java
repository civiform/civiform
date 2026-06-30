package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import models.VersionModel;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Helper class for performing validation related to creating or modifying program blocks. */
public final class ProgramBlockValidation {

  /**
   * Question types that may be chosen as the initial question on an enumerator block. Used by both
   * the server-side check in {@link #canAddQuestion} and the dropdown filter in {@code
   * CreateQuestionButton}; the two must stay in sync.
   */
  public static final ImmutableSet<QuestionType> VALID_INITIAL_QUESTION_TYPES =
      ImmutableSet.of(
          QuestionType.TEXT,
          QuestionType.RADIO_BUTTON,
          QuestionType.PHONE,
          QuestionType.NUMBER,
          QuestionType.NAME,
          QuestionType.ID,
          QuestionType.EMAIL,
          QuestionType.DROPDOWN,
          QuestionType.DATE,
          QuestionType.CURRENCY,
          QuestionType.ADDRESS);

  private final VersionModel version;
  private final ActiveAndDraftQuestions activeAndDraftQuestions;

  public ProgramBlockValidation(
      VersionModel version, ActiveAndDraftQuestions activeAndDraftQuestions) {
    this.version = checkNotNull(version);
    this.activeAndDraftQuestions = checkNotNull(activeAndDraftQuestions);
  }

  /**
   * Result of checking whether a question can be added to a specific block. Only ELIGIBLE means
   * that question can be added. All other states indicate that question is not eligible for the
   * given block.
   */
  public enum AddQuestionResult {
    // Question can be added to the block.
    ELIGIBLE,

    // Question already exists in one of the blocks of the given program.
    DUPLICATE,

    // Cannot add question to the block because the block contains a question
    // of special type (enumerator or file upload) that prohibits having
    // other questions in the block.
    BLOCK_IS_SINGLE_QUESTION,

    // Cannot add special question type (enumerator or file upload) to
    // non-empty block. These questions must be an only question in a block.
    CANT_ADD_SINGLE_BLOCK_QUESTION_TO_NON_EMPTY_BLOCK,

    // Cannot add question to the block because either block is a child of an enumerator
    // block and can contain only questions that themselves are children of the enumerator and
    // provided question is not; or the provided question is a child of an enumerator question while
    // the block is a regular block.
    ENUMERATOR_MISMATCH,
    QUESTION_TOMBSTONED,
    QUESTION_NOT_IN_ACTIVE_OR_DRAFT_STATE,

    // Cannot add question to the block because the question is an enumerator type, but the block is
    // not an enumerator block.
    ENUMERATOR_ON_NON_ENUMERATOR_BLOCK,

    // Cannot use this question as the initial question on an enumerator block because its
    // type is not in {@link #VALID_INITIAL_QUESTION_TYPES}.
    INVALID_INITIAL_QUESTION_TYPE
  }

  /**
   * Check whether given question can be added to the provided block.
   *
   * <p>This should be checked both during rendering to make sure admins don't see ineligible
   * questions when creating blocks. Also it should be checked during the actual server-side block
   * creating to ensure that no one can maliciously mess up data by sending specially crafted
   * request (or in case we mess up client-side checks). It also ensures that a question is not
   * tombstoned (marked for deletion) in the current draft.
   */
  public AddQuestionResult canAddQuestion(
      ProgramDefinition program,
      BlockDefinition block,
      QuestionDefinition question,
      boolean enumeratorImprovementsEnabled,
      boolean fileUploadQuestionImprovementsEnabled,
      boolean isInitialQuestionSelection) {
    if (version.getTombstonedQuestionNames().contains(question.getName())) {
      return AddQuestionResult.QUESTION_TOMBSTONED;
    }
    if (program.hasQuestion(question)) {
      return AddQuestionResult.DUPLICATE;
    }
    if ((!enumeratorImprovementsEnabled && block.hasEnumeratorQuestion())
        || (block.isFileUpload() && !fileUploadQuestionImprovementsEnabled)) {
      return AddQuestionResult.BLOCK_IS_SINGLE_QUESTION;
    }
    if (enumeratorImprovementsEnabled && question.isEnumerator() && !block.getIsEnumerator()) {
      return AddQuestionResult.ENUMERATOR_ON_NON_ENUMERATOR_BLOCK;
    }
    if (isInitialQuestionSelection
        && !VALID_INITIAL_QUESTION_TYPES.contains(question.getQuestionType())) {
      return AddQuestionResult.INVALID_INITIAL_QUESTION_TYPE;
    }
    if (block.getQuestionCount() > 0
        && isSingleBlockQuestion(question, fileUploadQuestionImprovementsEnabled)) {
      return AddQuestionResult.CANT_ADD_SINGLE_BLOCK_QUESTION_TO_NON_EMPTY_BLOCK;
    }
    if (!hasMatchingEnumeratorId(question, program, block, enumeratorImprovementsEnabled)) {
      return AddQuestionResult.ENUMERATOR_MISMATCH;
    }
    if (!activeAndDraftQuestions.getActiveQuestions().contains(question)
        && !activeAndDraftQuestions.getDraftQuestions().contains(question)) {
      return services.ProgramBlockValidation.AddQuestionResult
          .QUESTION_NOT_IN_ACTIVE_OR_DRAFT_STATE;
    }
    return AddQuestionResult.ELIGIBLE;
  }

  private boolean isSingleBlockQuestion(
      QuestionDefinition question, boolean fileUploadQuestionImprovementsEnabled) {
    return switch (question.getQuestionType()) {
      case ENUMERATOR -> true;
      case FILEUPLOAD -> !fileUploadQuestionImprovementsEnabled;
      default -> false;
    };
  }

  /**
   * Follow the {@link BlockDefinition#enumeratorId()} reference to the enumerator block definition,
   * and return the id of its {@link EnumeratorQuestionDefinition}.
   */
  private Optional<Long> getParentEnumeratorQuestionId(
      ProgramDefinition program, BlockDefinition block) {
    if (block.enumeratorId().isEmpty()) {
      return Optional.empty();
    }
    try {
      BlockDefinition enumeratorBlockDefinition =
          program.getBlockDefinition(block.enumeratorId().get());
      return enumeratorBlockDefinition.hasEnumeratorQuestion()
          ? Optional.of(enumeratorBlockDefinition.getEnumeratorQuestionDefinition().getId())
          : Optional.empty();
    } catch (ProgramBlockDefinitionNotFoundException e) {
      String errorMessage =
          String.format(
              "BlockDefinition %d has a broken enumerator block reference to id %d",
              block.id(), block.enumeratorId().get());
      throw new RuntimeException(errorMessage, e);
    }
  }

  /**
   * Checks whether a question's {@code enumeratorId} is compatible with the given block. A question
   * is compatible if any of the following is true:
   *
   * <ul>
   *   <li>Its enumeratorId equals the block's parent enumerator id — i.e. both are empty (top-level
   *       question on a top-level block) or both point to the same enumerator (repeated question on
   *       its matching repeated block).
   *   <li>It is a non-repeated question being added to a repeated block, and enumerator
   *       improvements are enabled.
   *   <li>Its enumeratorId points to the enumerator question on this block itself, and enumerator
   *       improvements are enabled (initial question case).
   * </ul>
   */
  private boolean hasMatchingEnumeratorId(
      QuestionDefinition question,
      ProgramDefinition program,
      BlockDefinition block,
      boolean enumeratorImprovementsEnabled) {
    Optional<Long> questionEnumeratorId = question.getEnumeratorId();

    // Standard case: question repeats under the block's parent enumerator or the question
    // is a top-level question (both the question and parent's enumeratorIds are empty).
    if (questionEnumeratorId.equals(getParentEnumeratorQuestionId(program, block))) {
      return true;
    }

    // At this point both items can't be empty, indicating at least one is associated with an
    // enumerator
    if (enumeratorImprovementsEnabled) {
      // Non-repeating question being considered for a repeated block, shown as an option
      // in the question bank sidebar before the question is copied
      if (questionEnumeratorId.isEmpty()) {
        return true;
      }
      // Initial question: its enumeratorId points to the enumerator on THIS block.
      if (block.hasEnumeratorQuestion()
          && questionEnumeratorId.equals(
              Optional.of(block.getEnumeratorQuestionDefinition().getId()))) {
        return true;
      }
    }

    return false;
  }
}
