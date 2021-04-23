package services.program;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import models.Program;
import services.LocalizationUtils;
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
  public abstract ImmutableMap<Locale, String> localizedName();

  /** A human readable description of a Program, localized for each supported locale. */
  public abstract ImmutableMap<Locale, String> localizedDescription();

  /** The list of {@link BlockDefinition}s that make up the program. */
  public abstract ImmutableList<BlockDefinition> blockDefinitions();

  /** The list of {@link ExportDefinition}s that make up the program. */
  public abstract ImmutableList<ExportDefinition> exportDefinitions();

  public String getLocalizedNameOrDefault(Locale locale) {
    try {
      return getLocalizedName(locale);
    } catch (TranslationNotFoundException e) {
      return getNameForDefaultLocale();
    }
  }

  /** The default locale for CiviForm is US English. */
  public String getNameForDefaultLocale() {
    try {
      return getLocalizedName(LocalizationUtils.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      // This should never happen - US English should always be supported.
      throw new RuntimeException(e);
    }
  }

  public Optional<String> maybeGetLocalizedName(Locale locale) {
    try {
      return Optional.of(getLocalizedName(locale));
    } catch (TranslationNotFoundException e) {
      return Optional.empty();
    }
  }

  public String getLocalizedName(Locale locale) throws TranslationNotFoundException {
    if (localizedName().containsKey(locale)) {
      return localizedName().get(locale);
    } else {
      throw new TranslationNotFoundException(id(), locale);
    }
  }

  public String getLocalizedDescriptionOrDefault(Locale locale) {
    try {
      return getLocalizedDescription(locale);
    } catch (TranslationNotFoundException e) {
      return getDescriptionForDefaultLocale();
    }
  }

  /** The default locale for CiviForm is US English. */
  public String getDescriptionForDefaultLocale() {
    try {
      return getLocalizedDescription(LocalizationUtils.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      // This should never happen - US English should always be supported.
      throw new RuntimeException(e);
    }
  }

  public Optional<String> maybeGetLocalizedDescription(Locale locale) {
    try {
      return Optional.of(getLocalizedDescription(locale));
    } catch (TranslationNotFoundException e) {
      return Optional.empty();
    }
  }

  public String getLocalizedDescription(Locale locale) throws TranslationNotFoundException {
    if (localizedDescription().containsKey(locale)) {
      return localizedDescription().get(locale);
    } else {
      throw new TranslationNotFoundException(id(), locale);
    }
  }

  /**
   * Get all the {@link Locale}s this program fully supports. A program fully supports a locale if:
   *
   * <ol>
   *   <li>The publicly-visible display name is localized for the locale
   *   <li>The publicly-visible description is localized for the locale
   *   <li>Every question in this program fully supports this locale
   * </ol>
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
        Sets.intersection(localizedName().keySet(), localizedDescription().keySet());
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
   * @throws ProgramBlockNotFoundException if no block matched the block id
   */
  public BlockDefinition getBlockDefinition(long blockDefinitionId)
      throws ProgramBlockNotFoundException {
    return blockDefinitions().stream()
        .filter(b -> b.id() == blockDefinitionId)
        .findAny()
        .orElseThrow(() -> new ProgramBlockNotFoundException(id(), blockDefinitionId));
  }

  public BlockDefinition getBlockDefinition(String blockId) throws ProgramBlockNotFoundException {
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

  /** Returns true if this program has a repeater block with the id. */
  public boolean hasRepeater(long repeaterId) {
    return blockDefinitions().stream()
        .anyMatch(
            blockDefinition -> blockDefinition.id() == repeaterId && blockDefinition.isRepeater());
  }

  /**
   * Get the block definitions associated with the repeater id. Returns an empty list if there are
   * none.
   */
  public ImmutableList<BlockDefinition> getBlockDefinitionsForRepeater(long repeaterId) {
    return blockDefinitions().stream()
        .filter(blockDefinition -> blockDefinition.repeaterId().equals(Optional.of(repeaterId)))
        .collect(ImmutableList.toImmutableList());
  }

  /** Get non-repeated block definitions. */
  public ImmutableList<BlockDefinition> getNonRepeatedBlockDefinitions() {
    return blockDefinitions().stream()
        .filter(blockDefinition -> blockDefinition.repeaterId().isEmpty())
        .collect(ImmutableList.toImmutableList());
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

    public abstract Builder setLocalizedName(ImmutableMap<Locale, String> localizedName);

    public abstract ImmutableMap.Builder<Locale, String> localizedNameBuilder();

    public abstract Builder setLocalizedDescription(
        ImmutableMap<Locale, String> localizedDescription);

    public abstract ImmutableMap.Builder<Locale, String> localizedDescriptionBuilder();

    public abstract Builder setBlockDefinitions(ImmutableList<BlockDefinition> blockDefinitions);

    public abstract Builder setExportDefinitions(ImmutableList<ExportDefinition> exportDefinitions);

    public abstract ImmutableList.Builder<BlockDefinition> blockDefinitionsBuilder();

    public abstract ImmutableList.Builder<ExportDefinition> exportDefinitionsBuilder();

    public abstract ProgramDefinition build();

    /**
     * Add a new localization for the program name. This will fail if a translation for the given
     * locale already exists.
     */
    public Builder addLocalizedName(Locale locale, String name) {
      localizedNameBuilder().put(locale, name);
      return this;
    }

    /**
     * Add a new localization for the program description. This will fail if a translation for the
     * given locale already exists.
     */
    public Builder addLocalizedDescription(Locale locale, String name) {
      localizedDescriptionBuilder().put(locale, name);
      return this;
    }

    /**
     * Update an existing localization for the program name. This will overwrite the old name for
     * that locale.
     */
    public Builder updateLocalizedName(
        ImmutableMap<Locale, String> existing, Locale locale, String name) {
      if (existing.containsKey(locale)) {
        setLocalizedName(LocalizationUtils.overwriteExistingTranslation(existing, locale, name));
      } else {
        addLocalizedName(locale, name);
      }
      return this;
    }

    /**
     * Update an existing localization for the program description. This will overwrite the old
     * description for that locale.
     */
    public Builder updateLocalizedDescription(
        ImmutableMap<Locale, String> existing, Locale locale, String description) {
      if (existing.containsKey(locale)) {
        setLocalizedDescription(
            LocalizationUtils.overwriteExistingTranslation(existing, locale, description));
      } else {
        addLocalizedDescription(locale, description);
      }
      return this;
    }

    public Builder addBlockDefinition(BlockDefinition blockDefinition) {
      blockDefinitionsBuilder().add(blockDefinition);
      return this;
    }

    public Builder addExportDefinition(ExportDefinition exportDefinition) {
      exportDefinitionsBuilder().add(exportDefinition);
      return this;
    }
  }
}
