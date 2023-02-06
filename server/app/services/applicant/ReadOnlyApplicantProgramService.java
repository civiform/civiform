package services.applicant;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.applicant.question.ApplicantQuestion;

/** Provides synchronous, read-only behavior relevant to an applicant for a specific program. */
public interface ReadOnlyApplicantProgramService {

  /** Returns the locked applicant data for this application. */
  ApplicantData getApplicantData();

  /** Returns the program title, localized to the applicant's preferred locale. */
  String getProgramTitle();

  /** Returns the ID of the program. */
  Long getProgramId();

  /**
   * Get the {@link Block}s for this program and applicant. This includes all blocks an applicant
   * must complete for this program, regardless of whether the block was filled out in this program
   * or a previous program. This will not include blocks that are hidden from the applicant (i.e.
   * they have a show/hide predicate).
   */
  ImmutableList<Block> getAllActiveBlocks();

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
   * Get a list of questions that have eligibility requirements from active blocks in the program.
   */
  ImmutableList<ApplicantQuestion> getActiveEligibilityQuestions();

  /** Get the block with the given block ID */
  Optional<Block> getBlock(String blockId);

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

  /** Returns summary data for each question in this application. */
  ImmutableList<AnswerData> getSummaryData();

  /** Get the string identifiers for all stored files for this application. */
  ImmutableList<String> getStoredFileKeys();

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

  /** Returns if the block eligibility criteria are met. */
  boolean isBlockEligible(String blockId);

  /**
   * Returns true if this program fully supports this applicant's preferred language, and false
   * otherwise.
   */
  boolean preferredLanguageSupported();
}
