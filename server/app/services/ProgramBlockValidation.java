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
    QUESTION_NOT_IN_ACTIVE_OR_DRAFT_STATE
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
      ProgramDefinition program, BlockDefinition block, QuestionDefinition question) {
    if (version.getTombstonedQuestionNames().contains(question.getName())) {
      return AddQuestionResult.QUESTION_TOMBSTONED;
    }
    if (program.hasQuestion(question)) {
      return AddQuestionResult.DUPLICATE;
    }
    if (block.isEnumerator() || block.isFileUpload()) {
      return AddQuestionResult.BLOCK_IS_SINGLE_QUESTION;
    }
    if (block.getQuestionCount() > 0 && isSingleBlockQuestion(question)) {
      return AddQuestionResult.CANT_ADD_SINGLE_BLOCK_QUESTION_TO_NON_EMPTY_BLOCK;
    }
    if (!question.getEnumeratorId().equals(getEnumeratorQuestionId(program, block))) {
      return AddQuestionResult.ENUMERATOR_MISMATCH;
    }
    if (!activeAndDraftQuestions.getActiveQuestions().contains(question)
        && !activeAndDraftQuestions.getDraftQuestions().contains(question)) {
      return services.ProgramBlockValidation.AddQuestionResult
          .QUESTION_NOT_IN_ACTIVE_OR_DRAFT_STATE;
    }
    return AddQuestionResult.ELIGIBLE;
  }

  private boolean isSingleBlockQuestion(QuestionDefinition question) {
    switch (question.getQuestionType()) {
      case ENUMERATOR:
      case FILEUPLOAD:
        return true;
      default:
        return false;
    }
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
      return Optional.of(enumeratorBlockDefinition.getQuestionDefinition(0).getId());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      String errorMessage =
          String.format(
              "BlockDefinition %d has a broken enumerator block reference to id %d",
              block.id(), block.enumeratorId().get());
      throw new RuntimeException(errorMessage, e);
    }
  }
}
