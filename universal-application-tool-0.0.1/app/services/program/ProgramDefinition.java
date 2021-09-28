package services.program;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.github.slugify.Slugify;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import models.DisplayMode;
import models.Program;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Defines configuration of a program. */
@AutoValue
public abstract class ProgramDefinition {

  private Optional<ImmutableSet<Long>> questionIds = Optional.empty();
  private Boolean hasOrderedBlockDefinitionsMemo;

  public static Builder builder() {
    return new AutoValue_ProgramDefinition.Builder();
  }

  /** Unique identifier for a ProgramDefinition. */
  public abstract long id();

  /**
   * Descriptive name of a Program, e.g. Car Tab Rebate Program. Different versions of the same
   * program are linked by their immutable name.
   */
  public abstract String adminName();

  /** A description of this program for the admin's reference. */
  public abstract String adminDescription();

  /** An external link to a page containing more details for this program. */
  public abstract String externalLink();

  /** The program's display mode. */
  public abstract DisplayMode displayMode();

  /**
   * Descriptive name of a Program, e.g. Car Tab Rebate Program, localized for each supported
   * locale.
   */
  public abstract LocalizedStrings localizedName();

  /** A human readable description of a Program, localized for each supported locale. */
  public abstract LocalizedStrings localizedDescription();

  /** The list of {@link BlockDefinition}s that make up the program. */
  public abstract ImmutableList<BlockDefinition> blockDefinitions();

  /** The list of {@link ExportDefinition}s that make up the program. */
  public abstract ImmutableList<ExportDefinition> exportDefinitions();

  /**
   * Returns a program definition with block definitions such that each enumerator block is
   * immediately followed by all of its repeated and nested repeated blocks. This method should be
   * used when {@link #hasOrderedBlockDefinitions()} is a precondition for manipulating blocks.
   *
   * <p>Programs created before early June 2021 may not satisfy this condition.
   */
  public ProgramDefinition orderBlockDefinitions() {
    if (!hasOrderedBlockDefinitions()) {
      ProgramDefinition orderedProgramDefinition =
          toBuilder()
              .setBlockDefinitions(orderBlockDefinitionsInner(getNonRepeatedBlockDefinitions()))
              .build();
      orderedProgramDefinition.hasOrderedBlockDefinitionsMemo = true;
      return orderedProgramDefinition;
    }
    return this;
  }

  private ImmutableList<BlockDefinition> orderBlockDefinitionsInner(
      ImmutableList<BlockDefinition> currentLevel) {
    ImmutableList.Builder<BlockDefinition> blockDefinitionBuilder = ImmutableList.builder();
    for (BlockDefinition blockDefinition : currentLevel) {
      blockDefinitionBuilder.add(blockDefinition);
      if (blockDefinition.isEnumerator()) {
        blockDefinitionBuilder.addAll(
            orderBlockDefinitionsInner(getBlockDefinitionsForEnumerator(blockDefinition.id())));
      }
    }
    return blockDefinitionBuilder.build();
  }

  /**
   * This method should be treated as VISIBLE FOR TESTING.
   *
   * <p>True indicates that each enumerator block in {@link #blockDefinitions()} is immediately
   * followed by all of its repeated and nested repeated blocks.
   */
  public boolean hasOrderedBlockDefinitions() {
    if (hasOrderedBlockDefinitionsMemo == null) {
      Deque<Long> enumeratorIds = new ArrayDeque<>();

      // Walk through the list of block definitions, checking that repeated and nested repeated
      // blocks
      // immediately follow their enumerator block.
      for (BlockDefinition blockDefinition : blockDefinitions()) {
        // Pop the stack until the enumerator id matches the top of the stack.
        while (enumeratorIds.size() > 0
            && !blockDefinition.enumeratorId().equals(Optional.of(enumeratorIds.peek()))) {
          enumeratorIds.pop();
        }

        // Early return if it still doesn't match, this is not ordered.
        if (!blockDefinition.enumeratorId().equals(Optional.ofNullable(enumeratorIds.peek()))) {
          hasOrderedBlockDefinitionsMemo = false;
          return false;
        }

        // Push this enumerator block's id
        if (blockDefinition.isEnumerator()) {
          enumeratorIds.push(blockDefinition.id());
        }
      }
      hasOrderedBlockDefinitionsMemo = true;
    }
    return hasOrderedBlockDefinitionsMemo;
  }

  /**
   * Checks whether this program has a valid block order, so that no predicate depends on a block
   * that appears after it.
   */
  public boolean hasValidPredicateOrdering() {
    Set<Long> previousQuestionIds = new HashSet<>();
    for (BlockDefinition b : blockDefinitions()) {
      // Check that all the predicate questions exist before this block.
      if (b.visibilityPredicate().isPresent()
          && !previousQuestionIds.containsAll(b.visibilityPredicate().get().getQuestions())) {
        return false;
      }
      b.programQuestionDefinitions().forEach(pqd -> previousQuestionIds.add(pqd.id()));
    }
    return true;
  }

  /**
   * Inserts the new block into the list of blocks such that it appears immediately after the last
   * repeated or nested repeated block with the same enumerator. If there is no enumerator, it is
   * added at the end.
   */
  public ProgramDefinition insertBlockDefinitionInTheRightPlace(BlockDefinition newBlockDefinition)
      throws ProgramBlockDefinitionNotFoundException {
    // Precondition: blocks have to be ordered
    if (!hasOrderedBlockDefinitions()) {
      return orderBlockDefinitions().insertBlockDefinitionInTheRightPlace(newBlockDefinition);
    }

    // Simple return for non-repeated block - just add to the end of the list
    if (!newBlockDefinition.isRepeated()) {
      return toBuilder().addBlockDefinition(newBlockDefinition).build();
    }
    BlockSlice enumeratorIndices = getBlockSlice(newBlockDefinition.enumeratorId().get());
    int insertIndex = enumeratorIndices.endIndex();

    // At this point, insertIndex is AFTER the last repeated or nested repeated block of the
    // enumerator. This might be off the end of the list.
    ImmutableList.Builder<BlockDefinition> newBlockDefinitionsBuilder = ImmutableList.builder();
    blockDefinitions().stream()
        .limit(insertIndex)
        .forEach(blockDefinition -> newBlockDefinitionsBuilder.add(blockDefinition));
    newBlockDefinitionsBuilder.add(newBlockDefinition);
    blockDefinitions().stream()
        .skip(insertIndex)
        .forEach(blockDefinition -> newBlockDefinitionsBuilder.add(blockDefinition));

    return toBuilder().setBlockDefinitions(newBlockDefinitionsBuilder.build()).build();
  }

  /**
   * Move the block in the direction specified if it is allowed. Blocks are not allowed to move
   * beyond the contiguous slice of repeated and nested repeated blocks of their enumerator.
   */
  public ProgramDefinition moveBlock(long blockId, Direction direction)
      throws ProgramBlockDefinitionNotFoundException, IllegalPredicateOrderingException {
    // Precondition: blocks have to be ordered
    if (!hasOrderedBlockDefinitions()) {
      return orderBlockDefinitions().moveBlock(blockId, direction);
    }

    BlockSlice blockSlice = getBlockSlice(blockId);
    Optional<BlockDefinition> otherBlock = findNextBlockWithSameEnumerator(blockSlice, direction);

    // Early return if there's no other block to swap with.
    if (otherBlock.isEmpty()) {
      return this;
    }

    BlockSlice otherBlockSlice = getBlockSlice(otherBlock.get().id());

    BlockSlice earlierSlice =
        blockSlice.startsBefore(otherBlockSlice) ? blockSlice : otherBlockSlice;
    BlockSlice latterSlice =
        blockSlice.startsBefore(otherBlockSlice) ? otherBlockSlice : blockSlice;

    ImmutableList.Builder<BlockDefinition> blocksBuilder = ImmutableList.builder();

    // Swap the two slices, assuming these slices are consecutive slices
    blockDefinitions().stream().limit(earlierSlice.startIndex()).forEach(blocksBuilder::add);
    blockDefinitions().stream()
        .skip(latterSlice.startIndex())
        .limit(latterSlice.endIndex() - latterSlice.startIndex())
        .forEach(blocksBuilder::add);
    blockDefinitions().stream()
        .skip(earlierSlice.startIndex())
        .limit(earlierSlice.endIndex() - earlierSlice.startIndex())
        .forEach(blocksBuilder::add);
    blockDefinitions().stream().skip(latterSlice.endIndex()).forEach(blocksBuilder::add);

    ProgramDefinition newProgram = toBuilder().setBlockDefinitions(blocksBuilder.build()).build();

    if (!newProgram.hasValidPredicateOrdering()) {
      throw new IllegalPredicateOrderingException(
          "This move is not possible - it would move a block condition before the question it"
              + " depends on");
    }

    return newProgram;
  }

  /**
   * Find the start and end indices of the slice of blocks associated with the block definition id.
   * For non-enumerator blocks, the slice of blocks associated with the block is just the block
   * itself. For enumerator blocks, the slice of blocks also contain all of its repeated and nested
   * repeated blocks.
   */
  private BlockSlice getBlockSlice(long blockId) throws ProgramBlockDefinitionNotFoundException {
    // Precondition: blocks have to be ordered
    if (!hasOrderedBlockDefinitions()) {
      return orderBlockDefinitions().getBlockSlice(blockId);
    }

    // Find the enumerator block
    int startIndex;
    try {
      startIndex =
          IntStream.range(0, blockDefinitions().size())
              .filter(i -> blockDefinitions().get(i).id() == blockId)
              .findFirst()
              .getAsInt();
    } catch (NoSuchElementException e) {
      // The enumerator id must correspond to a block within blocks.
      throw new ProgramBlockDefinitionNotFoundException(id(), blockId);
    }
    BlockDefinition blockDefinition = blockDefinitions().get(startIndex);
    int endIndex = startIndex + 1;

    // Early return for non-enumerator blocks
    if (!blockDefinition.isEnumerator()) {
      return BlockSlice.create(startIndex, endIndex);
    }

    // Increment the index until the ordered block definitions are not repeated or nested repeated
    // blocks of the enumerator
    Set<Long> enumeratorIds = new HashSet<>(Arrays.asList(blockId));
    while (endIndex < getBlockCount()) {
      BlockDefinition current = blockDefinitions().get(endIndex);
      if (current.enumeratorId().isEmpty()
          || !enumeratorIds.contains(current.enumeratorId().get())) {
        // This is not a repeated or nested repeated block of the enumerator
        break;
      }
      // Add nested enumerators into the set of enumerators
      if (current.isEnumerator()) {
        enumeratorIds.add(current.id());
      }
      endIndex++;
    }

    return BlockSlice.create(startIndex, endIndex);
  }

  /**
   * Find the next block with the same enumerator as the block at the start of the slice in the
   * specified direction. This block may not exist.
   */
  private Optional<BlockDefinition> findNextBlockWithSameEnumerator(
      BlockSlice blockSlice, Direction direction) {
    // Precondition: blocks have to be ordered
    if (!hasOrderedBlockDefinitions()) {
      return orderBlockDefinitions().findNextBlockWithSameEnumerator(blockSlice, direction);
    }

    BlockDefinition blockDefinition = blockDefinitions().get(blockSlice.startIndex());

    int index;
    switch (direction) {
      case UP:
        // Walk up from the start of the slice checking for the first block with
        // matching enumerator.
        index = blockSlice.startIndex() - 1;
        while (index >= 0) {
          if (blockDefinition.enumeratorId().equals(blockDefinitions().get(index).enumeratorId())) {
            break;
          }
          index--;
        }
        break;
      case DOWN:
      default:
        // Use the block after the end of the slice if it has the same enumerator,
        // or the other block does not exist
        BlockDefinition otherBlockDefinition = blockDefinitions().get(blockSlice.endIndex());
        index =
            blockDefinition.enumeratorId().equals(otherBlockDefinition.enumeratorId())
                ? blockSlice.endIndex()
                : -1;
    }
    return (0 <= index && index < getBlockCount())
        ? Optional.of(blockDefinitions().get(index))
        : Optional.empty();
  }

  /**
   * Get all the {@link Locale}s this program fully supports. A program fully supports a locale if:
   *
   * <ul>
   *   <li>The publicly-visible display name is localized for the locale
   *   <li>The publicly-visible description is localized for the locale
   *   <li>Every question in this program fully supports this locale
   * </ul>
   *
   * @return an {@link ImmutableSet} of all {@link Locale}s that are fully supported for this
   *     program
   */
  public ImmutableSet<Locale> getSupportedLocales() {
    ImmutableSet<ImmutableSet<Locale>> questionLocales =
        streamQuestionDefinitions()
            .map(QuestionDefinition::getSupportedLocales)
            .collect(toImmutableSet());

    Set<Locale> intersection =
        Sets.intersection(localizedName().locales(), localizedDescription().locales());
    for (ImmutableSet<Locale> set : questionLocales) {
      intersection = Sets.intersection(intersection, set);
    }
    return ImmutableSet.copyOf(intersection);
  }

  /** Returns the {@link QuestionDefinition} at the specified block and question indices. */
  public QuestionDefinition getQuestionDefinition(int blockIndex, int questionIndex) {
    return blockDefinitions().get(blockIndex).getQuestionDefinition(questionIndex);
  }

  /** Returns the {@link BlockDefinition} at the specified block index if available. */
  public Optional<BlockDefinition> getBlockDefinitionByIndex(int blockIndex) {
    if (blockIndex < 0 || blockIndex >= blockDefinitions().size()) {
      return Optional.empty();
    }
    return Optional.of(blockDefinitions().get(blockIndex));
  }

  /**
   * Get the {@link BlockDefinition} with the specified block definition id.
   *
   * @param blockDefinitionId the id of the block definition
   * @return the {@link BlockDefinition} with the specified block id
   * @throws ProgramBlockDefinitionNotFoundException if no block matched the block id
   */
  public BlockDefinition getBlockDefinition(long blockDefinitionId)
      throws ProgramBlockDefinitionNotFoundException {
    return blockDefinitions().stream()
        .filter(b -> b.id() == blockDefinitionId)
        .findAny()
        .orElseThrow(() -> new ProgramBlockDefinitionNotFoundException(id(), blockDefinitionId));
  }

  public BlockDefinition getBlockDefinition(String blockId)
      throws ProgramBlockDefinitionNotFoundException {
    // TODO: add a new exception for malformed blockId.
    // TODO: refactor this blockId parsing to a shared method somewhere with appropriate context.
    long blockDefinitionId =
        Splitter.on("-").splitToStream(blockId).map(Long::valueOf).findFirst().orElseThrow();
    return getBlockDefinition(blockDefinitionId);
  }

  /**
   * Get the last {@link BlockDefinition} of the program.
   *
   * @return the last {@link BlockDefinition}
   * @throws ProgramNeedsABlockException if the program has no blocks
   */
  public BlockDefinition getLastBlockDefinition() throws ProgramNeedsABlockException {
    return getBlockDefinitionByIndex(blockDefinitions().size() - 1)
        .orElseThrow(() -> new ProgramNeedsABlockException(id()));
  }

  /** Returns the max block definition id. */
  public long getMaxBlockDefinitionId() {
    return blockDefinitions().stream()
        .map(BlockDefinition::id)
        .max(Long::compareTo)
        .orElseGet(() -> 0L);
  }

  public int getBlockCount() {
    return blockDefinitions().size();
  }

  public String slug() {
    return new Slugify().slugify(this.adminName());
  }

  public int getQuestionCount() {
    return blockDefinitions().stream().mapToInt(BlockDefinition::getQuestionCount).sum();
  }

  /** True if a question with the given question's ID is in the program. */
  public boolean hasQuestion(QuestionDefinition question) {
    return hasQuestion(question.getId());
  }

  /** True if a question with the given questionId is in the program. */
  public boolean hasQuestion(long questionId) {
    if (questionIds.isEmpty()) {
      questionIds =
          Optional.of(
              blockDefinitions().stream()
                  .map(BlockDefinition::programQuestionDefinitions)
                  .flatMap(ImmutableList::stream)
                  .map(ProgramQuestionDefinition::id)
                  .collect(ImmutableSet.toImmutableSet()));
    }

    return questionIds.get().contains(questionId);
  }

  /** Returns true if this program has an enumerator block with the id. */
  public boolean hasEnumerator(long enumeratorId) {
    return blockDefinitions().stream()
        .anyMatch(
            blockDefinition ->
                blockDefinition.id() == enumeratorId && blockDefinition.isEnumerator());
  }

  /**
   * Get the block definitions associated with the enumerator id. Returns an empty list if there are
   * none.
   *
   * <p>The order of this list should reflect the sequential order of the blocks. This order is
   * depended upon in {@link ProgramDefinition#getAvailablePredicateQuestionDefinitions}.
   */
  public ImmutableList<BlockDefinition> getBlockDefinitionsForEnumerator(long enumeratorId) {
    return blockDefinitions().stream()
        .filter(blockDefinition -> blockDefinition.enumeratorId().equals(Optional.of(enumeratorId)))
        .collect(ImmutableList.toImmutableList());
  }

  /** Get non-repeated block definitions. */
  public ImmutableList<BlockDefinition> getNonRepeatedBlockDefinitions() {
    return blockDefinitions().stream()
        .filter(blockDefinition -> blockDefinition.enumeratorId().isEmpty())
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns a list of the question definitions that may be used to define predicates on the block
   * definition with the given ID.
   *
   * <p>The available question definitions for predicates satisfy ALL of the following:
   *
   * <ul>
   *   <li>In a block definition that comes sequentially before the given block definition.
   *   <li>In a block definition that either has the same enumerator ID as the given block
   *       definition, or has the same enumerator ID as some "parent" of the given block definition.
   *   <li>Is not an enumerator.
   * </ul>
   */
  public ImmutableList<QuestionDefinition> getAvailablePredicateQuestionDefinitions(long blockId)
      throws ProgramBlockDefinitionNotFoundException {
    ImmutableList.Builder<QuestionDefinition> builder = ImmutableList.builder();

    for (BlockDefinition blockDefinition :
        getAvailablePredicateBlockDefinitions(this.getBlockDefinition(blockId))) {
      builder.addAll(
          blockDefinition.programQuestionDefinitions().stream()
              .map(ProgramQuestionDefinition::getQuestionDefinition)
              .filter(ProgramDefinition::isPotentialPredicateQuestionDefinition)
              .collect(Collectors.toList()));
    }

    return builder.build();
  }

  private ImmutableList<BlockDefinition> getAvailablePredicateBlockDefinitions(
      BlockDefinition blockDefinition) throws ProgramBlockDefinitionNotFoundException {
    Optional<Long> maybeEnumeratorId = blockDefinition.enumeratorId();
    ImmutableList<BlockDefinition> siblingBlockDefinitions =
        maybeEnumeratorId
            .map(enumeratorBlockId -> this.getBlockDefinitionsForEnumerator(enumeratorBlockId))
            .orElse(getNonRepeatedBlockDefinitions());

    ImmutableList.Builder<BlockDefinition> builder = ImmutableList.builder();

    // If this block is repeated, recurse "upward". In other words, add all the available predicate
    // block definitions for its enumerator. Do this before adding its sibling block definitions to
    // maintain sequential order of the block definitions in the result.
    if (blockDefinition.isRepeated()) {
      builder.addAll(
          getAvailablePredicateBlockDefinitions(this.getBlockDefinition(maybeEnumeratorId.get())));
    }

    // Only include sequentially earlier block definitions.
    for (BlockDefinition siblingBlockDefinition : siblingBlockDefinitions) {
      // Stop adding block definitions once we reach this block.
      if (siblingBlockDefinition.id() == blockDefinition.id()) break;

      builder.add(siblingBlockDefinition);
    }

    return builder.build();
  }

  private static boolean isPotentialPredicateQuestionDefinition(QuestionDefinition qd) {
    // TODO(https://github.com/seattle-uat/civiform/issues/322): Add the following once STATIC
    //  questions are implemented.
    //  && qd.getQuestionTyp() != QuestionType.STATIC
    return !qd.isEnumerator() && qd.getQuestionType() != QuestionType.FILEUPLOAD;
  }

  public Program toProgram() {
    return new Program(this);
  }

  public abstract Builder toBuilder();

  public Stream<QuestionDefinition> streamQuestionDefinitions() {
    return blockDefinitions().stream()
        .flatMap(
            b ->
                b.programQuestionDefinitions().stream()
                    .map(ProgramQuestionDefinition::getQuestionDefinition));
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(long id);

    public abstract Builder setAdminName(String adminName);

    public abstract Builder setExternalLink(String externalLink);

    public abstract Builder setDisplayMode(DisplayMode displayMode);

    public abstract Builder setAdminDescription(String adminDescription);

    public abstract Builder setLocalizedName(LocalizedStrings localizedName);

    public abstract Builder setLocalizedDescription(LocalizedStrings localizedDescription);

    public abstract Builder setBlockDefinitions(ImmutableList<BlockDefinition> blockDefinitions);

    public abstract Builder setExportDefinitions(ImmutableList<ExportDefinition> exportDefinitions);

    public abstract ImmutableList.Builder<BlockDefinition> blockDefinitionsBuilder();

    public abstract ImmutableList.Builder<ExportDefinition> exportDefinitionsBuilder();

    public abstract LocalizedStrings.Builder localizedNameBuilder();

    public abstract LocalizedStrings.Builder localizedDescriptionBuilder();

    public abstract ProgramDefinition build();

    public Builder addBlockDefinition(BlockDefinition blockDefinition) {
      blockDefinitionsBuilder().add(blockDefinition);
      return this;
    }

    public Builder addExportDefinition(ExportDefinition exportDefinition) {
      exportDefinitionsBuilder().add(exportDefinition);
      return this;
    }

    public Builder addLocalizedName(Locale locale, String name) {
      localizedNameBuilder().put(locale, name);
      return this;
    }

    public Builder addLocalizedDescription(Locale locale, String description) {
      localizedDescriptionBuilder().put(locale, description);
      return this;
    }
  }

  public enum Direction {
    UP,
    DOWN;
  }

  @AutoValue
  abstract static class BlockSlice {
    abstract int startIndex();

    abstract int endIndex();

    static BlockSlice create(int startIndex, int endIndex) {
      return new AutoValue_ProgramDefinition_BlockSlice(startIndex, endIndex);
    }

    boolean startsBefore(BlockSlice other) {
      return startIndex() < other.startIndex();
    }
  }
}
