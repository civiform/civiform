package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.DbJsonB;
import java.util.List;
import java.util.Locale;
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
import services.LocalizationUtils;
import services.program.BlockDefinition;
import services.program.ExportDefinition;
import services.program.ProgramDefinition;

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

  @Constraints.Required @DbJsonB private ImmutableMap<Locale, String> localizedName;

  @Constraints.Required @DbJsonB private ImmutableMap<Locale, String> localizedDescription;

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
    this.localizedName = ImmutableMap.of(LocalizationUtils.DEFAULT_LOCALE, defaultDisplayName);
    this.localizedDescription =
        ImmutableMap.of(LocalizationUtils.DEFAULT_LOCALE, defaultDisplayDescription);
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
  }

  /** Populates {@link ProgramDefinition} from column values. */
  @PostLoad
  @PostPersist
  @PostUpdate
  public void loadProgramDefinition() {
    this.programDefinition =
        ProgramDefinition.builder()
            .setId(id)
            .setAdminName(name)
            .setAdminDescription(description)
            .setLocalizedName(localizedName)
            .setLocalizedDescription(localizedDescription)
            .setBlockDefinitions(blockDefinitions)
            .setExportDefinitions(exportDefinitions)
            .build();
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
}
