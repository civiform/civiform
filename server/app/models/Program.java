package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.program.StatusDefinitions;
import services.question.types.QuestionDefinition;

/**
 * An EBean mapped class that stores configuration for a specific benefits program.
 *
 * <p>A program contains a list of {@code BlockDefinition}s, each of which contains {@code
 * ProgramQuestionDefinition}s that reference a given {@code Question}. All questions referenced by
 * a program must be present in every {@code Version} which contains the program.
 *
 * <p>When an application is submitted, an immutable snapshot of the applicant's answers for the
 * program application are stored for that program and applicant in an {@code Application}.
 *
 * <p>Programs that aren't updated between versions are associated with multiple versions. I.e. A
 * program that is not updated will be carried over to a new version.
 */
@Entity
@Table(name = "programs")
public class Program extends BaseModel {

  private ProgramDefinition programDefinition;

  /** Different versions of the same program are linked by their immutable name. */
  @Constraints.Required private String name;

  @Constraints.Required private String description;

  /** Link to external site for this program. */
  @Constraints.Required private String externalLink;

  /** The program's display mode. */
  @Constraints.Required private String displayMode;

  // Not required - will be autofilled if not present.
  private String slug;

  @DbJsonB private LocalizedStrings localizedName;

  /**
   * legacyLocalizedName is the legacy storage column for program name translations. Programs
   * created before early May 2021 may use this, but all other programs should not.
   */
  @DbJsonB private ImmutableMap<Locale, String> legacyLocalizedName;

  @DbJsonB private LocalizedStrings localizedDescription;

  /**
   * legacyLocalizedDescription is the legacy storage column for program description translations.
   * Programs created before early May 2021 may use this, but all other programs should not.
   */
  @DbJsonB private ImmutableMap<Locale, String> legacyLocalizedDescription;

  @Constraints.Required @DbJson private ImmutableList<BlockDefinition> blockDefinitions;

  @Constraints.Required @DbJson private StatusDefinitions statusDefinitions;

  /** When was this program created. */
  @WhenCreated private Instant createTime;

  /**
   * When was this program last modified. Also updates every time any of the joinTable tables are
   * modified, for example a new Version is published.
   */
  @WhenModified private Instant lastModifiedTime;

  @Constraints.Required private ProgramType programType;

  @Constraints.Required private Boolean isEligibilityGating;

  @ManyToMany(mappedBy = "programs")
  private List<Version> versions;

  @OneToMany(mappedBy = "program")
  @OrderBy("id desc")
  private List<Application> applications;

  public ImmutableList<Version> getVersions() {
    return ImmutableList.copyOf(versions);
  }

  public ProgramDefinition getProgramDefinition() {
    return checkNotNull(this.programDefinition);
  }

  public StatusDefinitions getStatusDefinitions() {
    return checkNotNull(this.statusDefinitions);
  }

  public Program(ProgramDefinition definition) {
    this(definition, Optional.empty());
  }

  public Program(ProgramDefinition definition, Version version) {
    this(definition, Optional.of(version));
  }

  private Program(ProgramDefinition definition, Optional<Version> version) {
    this.programDefinition = definition;
    this.id = definition.id();
    this.name = definition.adminName();
    this.description = definition.adminDescription();
    this.externalLink = definition.externalLink();
    this.localizedName = definition.localizedName();
    this.localizedDescription = definition.localizedDescription();
    this.blockDefinitions = definition.blockDefinitions();
    this.statusDefinitions = definition.statusDefinitions();
    this.displayMode = definition.displayMode().getValue();
    this.programType = definition.programType();
    this.isEligibilityGating = definition.isEligibilityGating();

    orderBlockDefinitionsBeforeUpdate();

    if (version.isPresent()) {
      this.versions.add(version.get());
    }
  }

  /**
   * Construct a new Program object with the given program name, description, and block definitions.
   */
  public Program(
      String adminName,
      String adminDescription,
      String defaultDisplayName,
      String defaultDisplayDescription,
      String externalLink,
      String displayMode,
      ImmutableList<BlockDefinition> blockDefinitions,
      Version associatedVersion,
      ProgramType programType) {
    this.name = adminName;
    this.description = adminDescription;
    // A program is always created with the default CiviForm locale first, then localized.
    this.localizedName = LocalizedStrings.withDefaultValue(defaultDisplayName);
    this.localizedDescription = LocalizedStrings.withDefaultValue(defaultDisplayDescription);
    this.externalLink = externalLink;
    this.displayMode = displayMode;
    this.blockDefinitions = blockDefinitions;
    this.statusDefinitions = new StatusDefinitions();
    this.versions.add(associatedVersion);
    this.programType = programType;
    this.isEligibilityGating = true;
  }

  /** Populates column values from {@link ProgramDefinition} */
  @PreUpdate
  public void persistChangesToProgramDefinition() {
    id = programDefinition.id();
    name = programDefinition.adminName();
    externalLink = programDefinition.externalLink();
    description = programDefinition.adminDescription();
    localizedName = programDefinition.localizedName();
    localizedDescription = programDefinition.localizedDescription();
    blockDefinitions = programDefinition.blockDefinitions();
    statusDefinitions = programDefinition.statusDefinitions();
    slug = programDefinition.slug();
    displayMode = programDefinition.displayMode().getValue();
    programType = programDefinition.programType();
    isEligibilityGating = programDefinition.isEligibilityGating();

    orderBlockDefinitionsBeforeUpdate();
  }

  /** Populates {@link ProgramDefinition} from column values. */
  @PostLoad
  @PostPersist
  @PostUpdate
  public void loadProgramDefinition() {
    ProgramDefinition.Builder builder =
        ProgramDefinition.builder()
            .setId(id)
            .setAdminName(name)
            .setAdminDescription(description)
            .setBlockDefinitions(blockDefinitions)
            .setStatusDefinitions(statusDefinitions)
            .setExternalLink(externalLink)
            .setDisplayMode(DisplayMode.valueOf(displayMode))
            .setCreateTime(createTime)
            .setLastModifiedTime(lastModifiedTime)
            .setProgramType(programType)
            .setIsEligibilityGating(isEligibilityGating);

    setLocalizedName(builder);
    setLocalizedDescription(builder);
    this.programDefinition = builder.build();
  }

  /** The majority of programs should have `localizedName` and not `legacyLocalizedName`. */
  private Program setLocalizedName(ProgramDefinition.Builder builder) {
    if (localizedName != null) {
      builder.setLocalizedName(localizedName);
    } else {
      builder.setLocalizedName(LocalizedStrings.create(legacyLocalizedName));
    }
    return this;
  }

  /**
   * The majority of programs should have `localizedDescription` and not
   * `legacyLocalizedDescription`.
   */
  private Program setLocalizedDescription(ProgramDefinition.Builder builder) {
    if (localizedDescription != null) {
      builder.setLocalizedDescription(localizedDescription);
    } else {
      builder.setLocalizedDescription(LocalizedStrings.create(legacyLocalizedDescription));
    }
    return this;
  }

  /**
   * Returns submitted program applications sorted by descending application id. Applications are
   * obsolete if the applicant submitted the application more than once, but are included since all
   * submitted applications should be shown.
   */
  public ImmutableList<Application> getSubmittedApplications() {
    return applications.stream()
        .filter(
            application ->
                application.getLifecycleStage().equals(LifecycleStage.ACTIVE)
                    || application.getLifecycleStage().equals(LifecycleStage.OBSOLETE))
        .collect(ImmutableList.toImmutableList());
  }

  public String getSlug() {
    if (Strings.isNullOrEmpty(this.slug)) {
      this.slug = this.programDefinition.slug();
    }
    return this.slug;
  }

  /**
   * See {@link ProgramDefinition#orderBlockDefinitions} for why we need to order blocks.
   *
   * <p>This is used in {@link PreUpdate} but cannot be used when reading from storage because
   * {@link QuestionDefinition}s may not be present in the {@link ProgramDefinition}'s {@link
   * BlockDefinition}'s {@link services.program.ProgramQuestionDefinition}s.
   */
  private void orderBlockDefinitionsBeforeUpdate() {
    try {
      programDefinition = checkNotNull(programDefinition).orderBlockDefinitions();
      blockDefinitions = programDefinition.blockDefinitions();
    } catch (NoSuchElementException e) {
      // We are not able to check block order if the question definitions have not been
      // added to the program question definitions. If we can't check order, we don't
      // really need to make sure they're ordered, so this is a no-op.
    }
  }
}
