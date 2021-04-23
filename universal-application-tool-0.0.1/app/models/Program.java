package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.DbJsonB;
import java.util.List;
import java.util.Locale;
import javax.persistence.Entity;
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

  @Constraints.Required @DbJsonB private ImmutableMap<Locale, String> localizedName;

  @Constraints.Required @DbJsonB private ImmutableMap<Locale, String> localizedDescription;

  @Constraints.Required @DbJson private ImmutableList<BlockDefinition> blockDefinitions;

  @Constraints.Required private Long version;

  @Constraints.Required private LifecycleStage lifecycleStage;

  @Constraints.Required @DbJson private ImmutableList<ExportDefinition> exportDefinitions;

  @OneToMany(mappedBy = "program")
  private List<Application> applications;

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
    this.lifecycleStage = definition.lifecycleStage();
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
    // TODO(https://github.com/seattle-uat/civiform/issues/777): Allow the admin to
    // set localized strings for applicant-visible name and description.
    this.localizedName = ImmutableMap.of(LocalizationUtils.DEFAULT_LOCALE, defaultDisplayName);
    this.localizedDescription =
        ImmutableMap.of(LocalizationUtils.DEFAULT_LOCALE, defaultDisplayDescription);
    this.lifecycleStage = LifecycleStage.DRAFT;
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
    lifecycleStage = programDefinition.lifecycleStage();
    exportDefinitions = programDefinition.exportDefinitions();
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
            .setLifecycleStage(lifecycleStage)
            .setExportDefinitions(exportDefinitions)
            .build();
  }

  public ImmutableList<Application> getApplications() {
    return ImmutableList.copyOf(applications);
  }

  public LifecycleStage getLifecycleStage() {
    return lifecycleStage;
  }

  public void setLifecycleStage(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
    this.programDefinition =
        this.programDefinition.toBuilder().setLifecycleStage(lifecycleStage).build();
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
