package services.applicant;

import com.google.common.collect.ImmutableList;

import java.util.Optional;

/** Provides synchronous, read-only behavior relevant to an applicant for a specific program. */
interface ReadOnlyApplicantProgramService {

  /** Get the program's current Blocks for the applicant. */
  ImmutableList<Block> getCurrentBlockList();

  /** Get the block that comes after the block with the given ID if there is one. */
  Optional<Block> getBlockAfter(long blockId);

  /** Get the block that comes after the given block if there is one. */
  Optional<Block> getBlockAfter(Block block);

  /** Get the program block with the lowest index that has missing answer data if there is one. */
  Optional<Block> getFirstIncompleteBlock();
}
