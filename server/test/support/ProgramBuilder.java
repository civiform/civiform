package support;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import models.ApplicationStep;
import models.CategoryModel;
import models.DisplayMode;
import models.LifecycleStage;
import models.ProgramModel;
import models.ProgramNotificationPreference;
import models.QuestionModel;
import models.VersionModel;
import play.inject.Injector;
import repository.ApplicationStatusesRepository;
import repository.QuestionRepository;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.program.predicate.PredicateDefinition;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.statuses.StatusDefinitions;

/**
 * The ProgramBuilder can only be used by tests that have a database available because the programs
 * it builds are versioned with the version table, and are persisted in the database.
 *
 * <p>If any tests need to use the ProgramBuilder without a database, new `public static
 * ProgramBuilder newProgram` methods can be added to create programs that are not versioned, and do
 * not get persisted to the database.
 */
public class ProgramBuilder {

  private static final BlockDefinition EMPTY_FIRST_BLOCK =
      BlockDefinition.builder()
          .setId(1)
          .setName("Screen 1")
          .setDescription("Screen 1 description")
          .setLocalizedName(LocalizedStrings.withDefaultValue("Screen 1"))
          .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen 1 description"))
          .build();

  private static Injector injector;

  long programDefinitionId;
  ProgramDefinition.Builder builder;
  AtomicInteger numBlocks = new AtomicInteger(0);

  private ProgramBuilder(long programDefinitionId, ProgramDefinition.Builder builder) {
    this.programDefinitionId = programDefinitionId;
    this.builder = builder;
  }

  public static void setInjector(Injector i) {
    injector = i;
  }

  /**
   * Creates {@link ProgramBuilder} with a new {@link ProgramModel} with an empty name and
   * description, in draft state.
   */
  public static ProgramBuilder newDraftProgram() {
    return newDraftProgram("", "");
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} with an empty description and
   * a default short description, in draft state.
   */
  public static ProgramBuilder newDraftProgram(String name) {
    return newDraftProgram(name, "", "short description", DisplayMode.PUBLIC);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} with an empty description and
   * a default short description, in draft state with disabled visibility.
   */
  public static ProgramBuilder newDisabledDraftProgram(String name) {
    return newDraftProgram(name, "", "short description", DisplayMode.DISABLED);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} with a given description and a
   * default short description, in draft state.
   */
  public static ProgramBuilder newDraftProgram(String name, String description) {
    return newDraftProgram(name, description, "short description", DisplayMode.PUBLIC);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} with a given description and a
   * short description, in draft state.
   */
  public static ProgramBuilder newDraftProgram(
      String name, String description, String shortDescription) {
    return newDraftProgram(name, description, shortDescription, DisplayMode.PUBLIC);
  }

  /** Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in draft state. */
  public static ProgramBuilder newDraftProgram(
      String name, String description, String shortDescription, DisplayMode displayMode) {
    VersionRepository versionRepository = injector.instanceOf(VersionRepository.class);
    ProgramModel program =
        new ProgramModel(
            name,
            description,
            name,
            description,
            shortDescription,
            "",
            "https://usa.gov",
            displayMode.getValue(),
            ImmutableList.of(),
            ImmutableList.of(EMPTY_FIRST_BLOCK),
            versionRepository.getDraftVersionOrCreate(),
            ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            new ProgramAcls(),
            /* categories= */ ImmutableList.of(),
            ImmutableList.of(new ApplicationStep("title", "description")));
    program.save();
    ProgramDefinition.Builder builder =
        program.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of());
    return new ProgramBuilder(program.id, builder);
  }

  /** Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in draft state. */
  public static ProgramBuilder newDraftProgram(ProgramDefinition programDefinition) {
    VersionRepository versionRepository = injector.instanceOf(VersionRepository.class);
    ProgramModel program =
        new ProgramModel(programDefinition, versionRepository.getDraftVersionOrCreate());
    program.save();
    ProgramDefinition.Builder builder =
        program.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of());
    return new ProgramBuilder(program.id, builder);
  }

  /**
   * Wrap the provided {@link ProgramDefinition} in a {@link ProgramBuilder}.
   *
   * @param programDefinition the {@link ProgramDefinition} to create a {@link ProgramBuilder} for
   * @return the {@link ProgramBuilder}.
   */
  public static ProgramBuilder newBuilderFor(ProgramDefinition programDefinition) {
    ProgramDefinition.Builder builder = programDefinition.toBuilder();
    return new ProgramBuilder(programDefinition.id(), builder);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in active state, with blank
   * description and name.
   */
  public static ProgramBuilder newActiveProgram() {
    return newActiveProgram("");
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in the active state, with a
   * blank description.
   */
  public static ProgramBuilder newActiveProgram(String name) {
    return newActiveProgram(/* adminName= */ name, /* displayName= */ name, /* description= */ "");
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in the active state, with a
   * blank description and disabled.
   */
  public static ProgramBuilder newDisabledActiveProgram(String name) {
    return newActiveProgram(
        /* adminName= */ name,
        /* displayName= */ name,
        /* description= */ "",
        DisplayMode.DISABLED,
        ProgramType.DEFAULT);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in the active state, with a
   * blank description and hidden in index.
   */
  public static ProgramBuilder newActiveHiddenInIndexProgram(String name) {
    return newActiveProgram(
        /* adminName= */ name,
        /* displayName= */ name,
        /* description= */ "",
        DisplayMode.HIDDEN_IN_INDEX,
        ProgramType.DEFAULT);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in the active state, with a
   * blank description and visible only to Trusted Intermediaries.
   */
  public static ProgramBuilder newActiveTiOnlyProgram(String name) {
    return newActiveProgram(
        /* adminName= */ name,
        /* displayName= */ name,
        /* description= */ "",
        DisplayMode.TI_ONLY,
        ProgramType.DEFAULT);
  }

  /** Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in the active state. */
  public static ProgramBuilder newActiveProgram(String name, String description) {
    return newActiveProgram(/* adminName= */ name, /* displayName= */ name, description);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in the active state, with a
   * blank description.
   */
  public static ProgramBuilder newActiveProgramWithDisplayName(
      String adminName, String displayName) {
    return newActiveProgram(adminName, displayName, /* description= */ "");
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in the active state, with the
   * type ProgramType.COMMON_INTAKE_FORM.
   */
  public static ProgramBuilder newActiveCommonIntakeForm(String name) {
    return newActiveProgram(
        /* adminName= */ name,
        /* displayName= */ name,
        /* description= */ "",
        /* displayMode= */ DisplayMode.PUBLIC,
        ProgramType.COMMON_INTAKE_FORM);
  }

  /** Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in active state. */
  public static ProgramBuilder newActiveProgram(
      String adminName, String displayName, String description) {
    return newActiveProgram(
        adminName, displayName, description, DisplayMode.PUBLIC, ProgramType.DEFAULT);
  }

  /** Creates a {@link ProgramBuilder} with a new {@link ProgramModel} in active state. */
  public static ProgramBuilder newActiveProgram(
      String adminName,
      String displayName,
      String description,
      DisplayMode displayMode,
      ProgramType programType) {
    VersionRepository versionRepository = injector.instanceOf(VersionRepository.class);
    ProgramModel program =
        new ProgramModel(
            /* adminName */ adminName,
            /* adminDescription */ description,
            /* defaultDisplayName */ displayName,
            /* defaultDisplayDescription */ description,
            /* defaultShortDescription */ "short description",
            /* defaultConfirmationMessage */ "",
            /* externalLink */ "",
            /* displayMode */ displayMode.getValue(),
            /* notificationPreferences */ ImmutableList.of(),
            /* blockDefinitions */ ImmutableList.of(EMPTY_FIRST_BLOCK),
            /* associatedVersion */ versionRepository.getActiveVersion(),
            /* programType */ programType,
            /* eligibilityIsGating= */ true,
            /* ProgramAcls */ new ProgramAcls(),
            /* categories= */ ImmutableList.of(),
            /* appplicationSteps */ ImmutableList.of(new ApplicationStep("title", "description")));
    program.save();
    ProgramDefinition.Builder builder =
        program.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of());
    return new ProgramBuilder(program.id, builder);
  }

  /**
   * Creates a {@link ProgramBuilder} with a new {@link ProgramModel} associated with an obsolete
   * Version.
   */
  public static ProgramBuilder newObsoleteProgram(String adminName) {
    VersionModel obsoleteVersion = new VersionModel(LifecycleStage.OBSOLETE);
    obsoleteVersion.save();
    ProgramModel program =
        new ProgramModel(
            adminName,
            adminName,
            adminName,
            adminName,
            adminName,
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(EMPTY_FIRST_BLOCK),
            obsoleteVersion,
            ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            new ProgramAcls(),
            /* categories= */ ImmutableList.of(),
            ImmutableList.of(new ApplicationStep("title", "description")));
    program.save();
    ProgramDefinition.Builder builder =
        program.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of());
    return new ProgramBuilder(program.id, builder);
  }

  public ProgramBuilder withName(String name) {
    builder.setAdminName(name);
    return this;
  }

  public ProgramBuilder withDescription(String description) {
    builder.setAdminDescription(description);
    return this;
  }

  public ProgramBuilder withLocalizedName(Locale locale, String name) {
    builder.addLocalizedName(locale, name);
    return this;
  }

  public ProgramBuilder withLocalizedDescription(Locale locale, String description) {
    builder.addLocalizedDescription(locale, description);
    return this;
  }

  public ProgramBuilder withLocalizedShortDescription(Locale locale, String description) {
    builder.addLocalizedShortDescription(locale, description);
    return this;
  }

  public ProgramBuilder withApplicationSteps(ImmutableList<ApplicationStep> applicationSteps) {
    builder.setApplicationSteps(applicationSteps);
    return this;
  }

  public ProgramBuilder withLocalizedConfirmationMessage(Locale locale, String customText) {
    builder.addLocalizedConfirmationMessage(locale, customText);
    return this;
  }

  public ProgramBuilder setSummaryImageFileKey(Optional<String> summaryImageFileKey) {
    builder.setSummaryImageFileKey(summaryImageFileKey);
    return this;
  }

  public ProgramBuilder setLocalizedSummaryImageDescription(
      LocalizedStrings localizedSummaryImageDescription) {
    builder.setLocalizedSummaryImageDescription(Optional.of(localizedSummaryImageDescription));
    return this;
  }

  public ProgramBuilder withProgramType(ProgramType programType) {
    builder.setProgramType(programType);
    return this;
  }

  public ProgramBuilder setNotificationPreferences(
      ImmutableList<ProgramNotificationPreference> notificationPreferences) {
    builder.setNotificationPreferences(notificationPreferences);
    return this;
  }

  public ProgramBuilder withCategories(ImmutableList<CategoryModel> categories) {
    builder.setCategories(categories);
    return this;
  }

  public ProgramBuilder withAcls(ProgramAcls programAcls) {
    builder.setAcls(programAcls);
    return this;
  }

  /**
   * Creates a {@link BlockBuilder} with this {@link ProgramBuilder} with empty name and
   * description.
   */
  public BlockBuilder withBlock() {
    long blockId = Long.valueOf(numBlocks.incrementAndGet());
    return BlockBuilder.newBlock(this, blockId, "", "", Optional.empty());
  }

  /** Creates a {@link BlockBuilder} with this {@link ProgramBuilder} with empty description. */
  public BlockBuilder withBlock(String name) {
    long blockId = Long.valueOf(numBlocks.incrementAndGet());
    return BlockBuilder.newBlock(this, blockId, name, "", Optional.empty());
  }

  /** Creates a {@link BlockBuilder} with this {@link ProgramBuilder}. */
  public BlockBuilder withBlock(String name, String description) {
    long blockId = Long.valueOf(numBlocks.incrementAndGet());
    return BlockBuilder.newBlock(this, blockId, name, description, Optional.empty());
  }

  /** Returns the {@link ProgramDefinition} built from this {@link ProgramBuilder}. */
  public ProgramDefinition buildDefinition() {
    ProgramModel program = build();
    return program.getProgramDefinition();
  }

  /** Returns the {@link ProgramModel} built from this {@link ProgramBuilder}. */
  public ProgramModel build() {
    ProgramDefinition programDefinition = builder.build();
    if (programDefinition.blockDefinitions().isEmpty()) {
      return withBlock().build();
    }

    ProgramModel program = programDefinition.toProgram();
    program.update();
    ApplicationStatusesRepository appStatusRepo =
        injector.instanceOf(ApplicationStatusesRepository.class);
    appStatusRepo.createOrUpdateStatusDefinitions(
        programDefinition.adminName(), new StatusDefinitions());
    return program;
  }

  /**
   * Fluently creates {@link BlockDefinition}s with {@link ProgramBuilder}. This class has no public
   * constructors and should only be used with {@link ProgramBuilder}.
   */
  public static class BlockBuilder {

    private final ProgramBuilder programBuilder;
    private BlockDefinition.Builder blockDefBuilder;

    private BlockBuilder(ProgramBuilder programBuilder) {
      this.programBuilder = programBuilder;
      blockDefBuilder = BlockDefinition.builder();
    }

    private static BlockBuilder newBlock(
        ProgramBuilder programBuilder,
        long id,
        String name,
        String description,
        Optional<Long> enumeratorId) {
      BlockBuilder blockBuilder = new BlockBuilder(programBuilder);
      blockBuilder.blockDefBuilder =
          BlockDefinition.builder()
              .setId(id)
              .setName(name)
              .setDescription(description)
              .setLocalizedName(LocalizedStrings.withDefaultValue(name))
              .setLocalizedDescription(LocalizedStrings.withDefaultValue(description))
              .setEnumeratorId(enumeratorId);
      return blockBuilder;
    }

    public BlockBuilder withName(String name) {
      blockDefBuilder.setName(name);
      blockDefBuilder.setLocalizedName(LocalizedStrings.withDefaultValue(name));
      return this;
    }

    public BlockBuilder withDescription(String description) {
      blockDefBuilder.setDescription(description);
      blockDefBuilder.setLocalizedDescription(LocalizedStrings.withDefaultValue(description));
      return this;
    }

    public BlockBuilder withEligibilityDefinition(EligibilityDefinition eligibility) {
      blockDefBuilder.setEligibilityDefinition(eligibility);
      return this;
    }

    public BlockBuilder withVisibilityPredicate(PredicateDefinition predicate) {
      blockDefBuilder.setVisibilityPredicate(predicate);
      return this;
    }

    /** Add a required question to the block. */
    public BlockBuilder withRequiredQuestion(QuestionModel question) {
      QuestionRepository questionRepository = injector.instanceOf(QuestionRepository.class);
      blockDefBuilder.addQuestion(
          ProgramQuestionDefinition.create(
              questionRepository.getQuestionDefinition(question),
              Optional.of(programBuilder.programDefinitionId)));
      return this;
    }

    /** Add a required address question that has correction enabled to the block. */
    public BlockBuilder withRequiredCorrectedAddressQuestion(QuestionModel question) {
      QuestionRepository questionRepository = injector.instanceOf(QuestionRepository.class);
      if (!(questionRepository.getQuestionDefinition(question)
          instanceof AddressQuestionDefinition)) {
        throw new IllegalArgumentException("Only address questions can be address corrected.");
      }

      blockDefBuilder.addQuestion(
          ProgramQuestionDefinition.create(
              questionRepository.getQuestionDefinition(question),
              Optional.of(programBuilder.programDefinitionId),
              /* optional= */ true,
              /* addressCorrectionEnabled= */ true));
      return this;
    }

    public BlockBuilder withOptionalQuestion(QuestionModel question) {
      QuestionRepository questionRepository = injector.instanceOf(QuestionRepository.class);
      return withOptionalQuestion(questionRepository.getQuestionDefinition(question));
    }

    public BlockBuilder withOptionalQuestion(QuestionDefinition question) {
      blockDefBuilder.addQuestion(
          ProgramQuestionDefinition.create(
                  question, Optional.of(programBuilder.programDefinitionId))
              .setOptional(true));
      return this;
    }

    /** Add a required question definition to the block. */
    public BlockBuilder withRequiredQuestionDefinition(QuestionDefinition question) {
      return withQuestionDefinition(question, false);
    }

    public BlockBuilder withQuestionDefinition(QuestionDefinition question, boolean optional) {
      blockDefBuilder.addQuestion(
          ProgramQuestionDefinition.create(
                  question, Optional.of(programBuilder.programDefinitionId))
              .setOptional(optional));
      return this;
    }

    public BlockBuilder withRequiredQuestions(QuestionModel... questions) {
      return withRequiredQuestions(ImmutableList.copyOf(questions));
    }

    public BlockBuilder withRequiredQuestions(ImmutableList<QuestionModel> questions) {
      QuestionRepository questionRepository = injector.instanceOf(QuestionRepository.class);
      return withRequiredQuestionDefinitions(
          questions.stream()
              .map(q -> questionRepository.getQuestionDefinition(q))
              .collect(ImmutableList.toImmutableList()));
    }

    public BlockBuilder withRequiredQuestionDefinitions(
        ImmutableList<QuestionDefinition> questions) {
      ImmutableList<ProgramQuestionDefinition> pqds =
          questions.stream()
              .map(
                  questionDefinition ->
                      ProgramQuestionDefinition.create(
                          questionDefinition, Optional.of(programBuilder.programDefinitionId)))
              .collect(ImmutableList.toImmutableList());
      blockDefBuilder.setProgramQuestionDefinitions(pqds);
      return this;
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder} with an empty name and description.
     */
    public BlockBuilder withBlock() {
      return withBlock("", "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder} with an empty description.
     */
    public BlockBuilder withBlock(String name) {
      return withBlock(name, "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new {@link support.ProgramBuilder.BlockBuilder}.
     */
    public BlockBuilder withBlock(String name, String description) {
      programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
      long blockId = Long.valueOf(programBuilder.numBlocks.incrementAndGet());
      return BlockBuilder.newBlock(programBuilder, blockId, name, description, Optional.empty());
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that has this block as its
     * enumerator, with an empty name and description.
     */
    public BlockBuilder withRepeatedBlock() {
      return withRepeatedBlock("", "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that has this block as its
     * enumerator, with an empty description.
     */
    public BlockBuilder withRepeatedBlock(String name) {
      return withRepeatedBlock(name, "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that has this block as its
     * enumerator.
     */
    public BlockBuilder withRepeatedBlock(String name, String description) {
      BlockDefinition thisBlock = blockDefBuilder.build();
      if (!thisBlock.isEnumerator()) {
        throw new RuntimeException(
            "Cannot create a repeated block if this block is not an enumerator.");
      }
      programBuilder.builder.addBlockDefinition(thisBlock);
      long blockId = Long.valueOf(programBuilder.numBlocks.incrementAndGet());
      return BlockBuilder.newBlock(
          programBuilder, blockId, name, description, Optional.of(thisBlock.id()));
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that shares an enumerator
     * block with this block, with an empty name and description.
     */
    public BlockBuilder withAnotherRepeatedBlock() {
      return withAnotherRepeatedBlock("", "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that shares an enumerator
     * block with this block, with an empty description.
     */
    public BlockBuilder withAnotherRepeatedBlock(String name) {
      return withAnotherRepeatedBlock(name, "");
    }

    /**
     * Adds this {@link support.ProgramBuilder.BlockBuilder} to the {@link ProgramBuilder} and
     * starts a new repeated {@link support.ProgramBuilder.BlockBuilder} that shares an enumerator
     * block with this block.
     */
    public BlockBuilder withAnotherRepeatedBlock(String name, String description) {
      BlockDefinition thisBlock = blockDefBuilder.build();
      if (!thisBlock.isRepeated()) {
        throw new RuntimeException(
            "Cannot create a another repeated block if this block is not repeated.");
      }
      programBuilder.builder.addBlockDefinition(thisBlock);
      long blockId = Long.valueOf(programBuilder.numBlocks.incrementAndGet());
      return BlockBuilder.newBlock(
          programBuilder, blockId, name, description, thisBlock.enumeratorId());
    }

    /**
     * Returns the {@link ProgramDefinition} built from the {@link ProgramBuilder} with this {@link
     * BlockBuilder}.
     */
    public ProgramDefinition buildDefinition() {
      return build().getProgramDefinition();
    }

    /**
     * Returns the {@link ProgramModel} built from the {@link ProgramBuilder} with this {@link
     * BlockBuilder}.
     */
    public ProgramModel build() {
      programBuilder.builder.addBlockDefinition(blockDefBuilder.build());
      return programBuilder.build();
    }
  }
}
