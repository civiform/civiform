package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.DbJsonB;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ExportDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

/** The ebeans mapped class for the program object. */
@Entity
@Table(name = "programs")
public class Program extends BaseModel {

  private ProgramDefinition programDefinition;

  /** Different versions of the same program are linked by their immutable name. */
  @Constraints.Required private String name;

  @Constraints.Required private String description;

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

  @Constraints.Required @DbJson private ImmutableList<ExportDefinition> exportDefinitions;

  @ManyToMany
  @JoinTable(name = "versions_programs")
  private List<Version> versions;

  @OneToMany(mappedBy = "program")
  private List<Application> applications;

  public ImmutableList<Version> getVersions() {
    return ImmutableList.copyOf(versions);
  }

  public ProgramDefinition getProgramDefinition() {
    return checkNotNull(this.programDefinition);
  }

  public Program(ProgramDefinition definition) {
    this.programDefinition = definition;
    this.id = definition.id();
    this.name = definition.adminName();
    this.description = definition.adminDescription();
    this.localizedName = definition.localizedName();
    this.localizedDescription = definition.localizedDescription();
    this.blockDefinitions = definition.blockDefinitions();
    this.exportDefinitions = definition.exportDefinitions();

    reorderBlockDefinitionsBeforeUpdate();
  }

  /**
   * Construct a new Program object with the given program name and description, and with an empty
   * block named Block 1.
   */
  public Program(
      String adminName,
      String adminDescription,
      String defaultDisplayName,
      String defaultDisplayDescription) {
    this.name = adminName;
    this.description = adminDescription;
    // A program is always created with the default CiviForm locale first, then localized.
    this.localizedName = LocalizedStrings.withDefaultValue(defaultDisplayName);
    this.localizedDescription = LocalizedStrings.withDefaultValue(defaultDisplayDescription);
    BlockDefinition emptyBlock =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Block 1")
            .setDescription("Block 1 Description")
            .setProgramQuestionDefinitions(ImmutableList.of())
            .build();
    this.exportDefinitions = ImmutableList.of();
    this.blockDefinitions = ImmutableList.of(emptyBlock);
  }

  /** Populates column values from {@link ProgramDefinition} */
  @PreUpdate
  public void persistChangesToProgramDefinition() {
    id = programDefinition.id();
    name = programDefinition.adminName();
    description = programDefinition.adminDescription();
    localizedName = programDefinition.localizedName();
    localizedDescription = programDefinition.localizedDescription();
    blockDefinitions = programDefinition.blockDefinitions();
    exportDefinitions = programDefinition.exportDefinitions();
    slug = programDefinition.slug();

    reorderBlockDefinitionsBeforeUpdate();
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
            .setExportDefinitions(exportDefinitions);

    setLocalizedName(builder);
    setLocalizedDescription(builder);
    this.programDefinition = builder.build();
  }

  /** The majority of programs should have `localizedName` and not `legacyLocalizedName`. */
  private void setLocalizedName(ProgramDefinition.Builder builder) {
    if (localizedName != null) {
      builder.setLocalizedName(localizedName);
      return;
    }
    builder.setLocalizedName(LocalizedStrings.create(legacyLocalizedName));
  }

  /**
   * The majority of programs should have `localizedDescription` and not
   * `legacyLocalizedDescription`.
   */
  private void setLocalizedDescription(ProgramDefinition.Builder builder) {
    if (localizedDescription != null) {
      builder.setLocalizedDescription(localizedDescription);
      return;
    }
    builder.setLocalizedDescription(LocalizedStrings.create(legacyLocalizedDescription));
  }

  public ImmutableList<Application> getApplications() {
    return ImmutableList.copyOf(applications);
  }

  public void addVersion(Version version) {
    this.versions.add(version);
  }

  public String getSlug() {
    if (Strings.isNullOrEmpty(this.slug)) {
      this.slug = this.programDefinition.slug();
    }
    return this.slug;
  }

  /**
   * See {@link ProgramDefinition#orderBlockDefinitions} for why we need to reorder blocks.
   *
   * <p>This is used in {@link PreUpdate} but cannot be used when reading from storage because
   * {@link QuestionDefinition}s may not be present in the {@link ProgramDefinition}'s {@link
   * BlockDefinition}'s {@link services.program.ProgramQuestionDefinition}s.
   */
  private void reorderBlockDefinitionsBeforeUpdate() {
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
