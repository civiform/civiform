package services.applicant;

import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** Provides synchronous, read-only behavior relevant to an applicant for a specific program. */
public interface ReadOnlyApplicantProgramService {

  /** Get the program's current Blocks for the applicant. */
  ImmutableList<Block> getCurrentBlockList();

  /** Get the block with the given block ID */
  Optional<Block> getBlock(String blockId);

  /** Get the block that comes after the block with the given ID if there is one. */
  Optional<Block> getBlockAfter(String blockId);

  /** Get the block that comes after the given block if there is one. */
  Optional<Block> getBlockAfter(Block block);

  /** Get the program block with the lowest index that has missing answer data if there is one. */
  Optional<Block> getFirstIncompleteBlock();

  /** Returns summary data for each question in this application. */
  ImmutableList<AnswerData> getSummaryData();

  /**
   * Returns true if this program fully supports this applicant's preferred language, and false
   * otherwise.
   */
  boolean preferredLanguageSupported();
}
