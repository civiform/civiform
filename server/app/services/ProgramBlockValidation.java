package services;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import models.VersionModel;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;

/** Helper class for performing validation related to creating or modifying program blocks. */
public final class ProgramBlockValidation {

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

    // Cannot add question to the block because the question is not an enumerator type,
    // and the block is an enumerator block.  Does not apply to initial questions.
    NON_ENUMERATOR_ON_ENUMERATOR_BLOCK
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
      boolean fileUploadQuestionImprovementsEnabled) {
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
    if (block.getQuestionCount() > 0
        && isSingleBlockQuestion(question, fileUploadQuestionImprovementsEnabled)) {
      return AddQuestionResult.CANT_ADD_SINGLE_BLOCK_QUESTION_TO_NON_EMPTY_BLOCK;
    }
    if (!question.getEnumeratorId().equals(getEnumeratorQuestionId(program, block))
        && !(enumeratorImprovementsEnabled && question.getEnumeratorId().isEmpty())) {
      return AddQuestionResult.ENUMERATOR_MISMATCH;
    }
    if (!activeAndDraftQuestions.getActiveQuestions().contains(question)
        && !activeAndDraftQuestions.getDraftQuestions().contains(question)) {
      return services.ProgramBlockValidation.AddQuestionResult
          .QUESTION_NOT_IN_ACTIVE_OR_DRAFT_STATE;
    }
    return AddQuestionResult.ELIGIBLE;
  }

  public AddQuestionResult canAddQuestionToEnumeratorBlock(
      ProgramDefinition program,
      BlockDefinition block,
      QuestionDefinition question,
      boolean enumeratorImprovementsEnabled,
      boolean fileUploadQuestionImprovementsEnabled) {
    if (!question.isEnumerator()) {
      return AddQuestionResult.NON_ENUMERATOR_ON_ENUMERATOR_BLOCK;
    } else {
      return canAddQuestion(
          program,
          block,
          question,
          enumeratorImprovementsEnabled,
          fileUploadQuestionImprovementsEnabled);
    }
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
  private Optional<Long> getEnumeratorQuestionId(ProgramDefinition program, BlockDefinition block) {
    if (block.enumeratorId().isEmpty()) {
      return Optional.empty();
    }
    try {
      BlockDefinition enumeratorBlockDefinition =
          program.getBlockDefinition(block.enumeratorId().get());
      return enumeratorBlockDefinition.hasEnumeratorQuestion()
          ? Optional.of(enumeratorBlockDefinition.getQuestionDefinition(0).getId())
          : Optional.empty();
    } catch (ProgramBlockDefinitionNotFoundException e) {
      String errorMessage =
          String.format(
              "BlockDefinition %d has a broken enumerator block reference to id %d",
              block.id(), block.enumeratorId().get());
      throw new RuntimeException(errorMessage, e);
    }
  }
}
