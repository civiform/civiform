package services.applicant;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.stream.Stream;
import services.LocalizedStrings;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramType;

/** Provides synchronous, read-only behavior relevant to an applicant for a specific program. */
public interface ReadOnlyApplicantProgramService {

  /** Returns the locked applicant data for this application. */
  ApplicantData getApplicantData();

  /** Returns the program title, localized to the applicant's preferred locale. */
  String getProgramTitle();

  /** Returns the program description, localized to the applicant's preferred locale. */
  public String getProgramDescription();

  /** Returns the ID of the program. */
  Long getProgramId();

  /** Returns the ProgramType of the program. */
  ProgramType getProgramType();

  /**
   * Returns a custom message for the confirmation screen that renders after an applicant submits an
   * application. If a custom message is not set, returns an empty string.
   */
  LocalizedStrings getCustomConfirmationMessage();

  /**
   * Get a list of all {@link ApplicantQuestion}s in the program.
   *
   * @return A stream of the questions in the program.
   */
  Stream<ApplicantQuestion> getAllQuestions();

  /**
   * Get the {@link Block}s for this program and applicant. This includes all blocks an applicant
   * must complete for this program, regardless of whether the block was filled out in this program
   * or a previous program. This will not include blocks that are hidden from the applicant (i.e.
   * they have a show/hide predicate).
   */
  ImmutableList<Block> getAllActiveBlocks();

  /**
   * Get the {@link Block}s for this program and applicant. This only includes blocks that are
   * hidden from the applicant (i.e.they have a show/hide predicate).
   */
  ImmutableList<Block> getAllHiddenBlocks();

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
  ImmutableList<Block> getInProgressBlocks();

  /**
   * Get the count of blocks in this program that the applicant should see which have all their
   * questions answered or optional questions skipped.
   *
   * @return the count of active blocks completed in this program.
   */
  int getActiveAndCompletedInProgramBlockCount();

  /**
   * Returns whether the applicant should see the eligibility message. This is based on whether the
   * applicant has answered any eligibility questions in the program AND whether eligibility is
   * gating or the application is eligible. When non-gating eligibility is enabled and the person
   * doesn't qualify, we don't show them a message.
   */
  boolean shouldDisplayEligibilityMessage();

  /**
   * Get a list of questions that the applicant is currently not eligible for based on their answers
   * from active blocks in the program.
   */
  ImmutableList<ApplicantQuestion> getIneligibleQuestions();

  /** Get the hidden block with the given block ID if there is one. It is empty if there isn't. */
  Optional<Block> getHiddenBlock(String blockId);

  /**
   * Get the active block with the given block ID if there is one. It is empty if there isn't.
   * Active block is the block an applicant must complete for this program. This will not include
   * blocks that are hidden from the applicant.
   */
  Optional<Block> getActiveBlock(String blockId);

  /**
   * Get the next in-progress block that comes after the block with the given ID if there is one.
   */
  Optional<Block> getInProgressBlockAfter(String blockId);

  /** Returns the index of the given block in the context of all blocks of the program. */
  int getBlockIndex(String blockId);

  /**
   * Get the program block with the lowest index that has missing answer data if there is one.
   * Static questions are marked as incomplete.
   */
  Optional<Block> getFirstIncompleteOrStaticBlock();

  /**
   * Get the program block with the lowest index that has missing answer data if there is one.
   * Static questions are marked as complete.
   */
  Optional<Block> getFirstIncompleteBlockExcludingStatic();

  /**
   * Returns summary data for each question in this application. Includes blocks that are hidden
   * from the applicant due to visibility conditions.
   */
  ImmutableList<AnswerData> getSummaryDataAllQuestions();

  /**
   * Returns summary data for each question in the active blocks in this application. Active block
   * is the block an applicant must complete for this program. This will not include blocks that are
   * hidden from the applicant.
   */
  ImmutableList<AnswerData> getSummaryDataOnlyActive();

  /**
   * Returns summary data for each question in the hidden blocks in this application. Hidden block
   * is the block not visible to the applicant based on the visibility setting by the admin. This
   * will not include blocks that are active.
   */
  ImmutableList<AnswerData> getSummaryDataOnlyHidden();

  /** Get the string identifiers for all stored files for this application. */
  ImmutableList<String> getStoredFileKeys(boolean multipleUploadsEnabled);

  /**
   * Returns if all Program eligibility criteria are met. This will return false in some cases where
   * the eligibility questions haven't yet been answered.
   */
  boolean isApplicationEligible();

  /**
   * True if any of the answered questions in the program are not eligible, even if the application
   * hasn't yet been completed.
   */
  boolean isApplicationNotEligible();

  /** Returns if the block has an eligibility predicate. */
  boolean blockHasEligibilityPredicate(String blockId);

  /** Returns if the active block eligibility criteria are met. */
  boolean isActiveBlockEligible(String blockId);

  /**
   * Returns true if this program fully supports this applicant's preferred language, and false
   * otherwise.
   */
  boolean preferredLanguageSupported();
}
