package services.program;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.github.slugify.Slugify;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import models.Program;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;

@AutoValue
public abstract class ProgramDefinition {

  private Optional<ImmutableSet<Long>> questionIds = Optional.empty();

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
   * definition with the given ID. In general, these are question definitions that are in block
   * definitions that come sequentially before the given block definition.
   */
  public ImmutableList<QuestionDefinition> getAvailablePredicateQuestionDefinitions(long blockId) {
    ImmutableList.Builder<QuestionDefinition> builder = new ImmutableList.Builder<>();

    for (BlockDefinition blockDefinition : getNonRepeatedBlockDefinitions()) {
      if (blockDefinition.id() == blockId) break;

      builder.addAll(
          blockDefinition.programQuestionDefinitions().stream()
              .map(ProgramQuestionDefinition::getQuestionDefinition)
              .collect(Collectors.toList()));
    }

    return builder.build();
  }

  public Program toProgram() {
    return new Program(this);
  }

  public abstract Builder toBuilder();

  private Stream<QuestionDefinition> streamQuestionDefinitions() {
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
}
