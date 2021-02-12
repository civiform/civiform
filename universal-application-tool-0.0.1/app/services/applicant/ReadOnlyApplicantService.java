package services.applicant;

interface ReadOnlyApplicantService {
  ImmutableList<Block> getCurrentBlockList();

  Block getBlockAfter(long blockId);
  Block getBlockAfter(Block block);

  Block getFirstIncompleteBlock();
}
