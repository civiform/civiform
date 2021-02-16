package models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.ebean.annotation.DbJson;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.program.ProgramDefinition;

/** The ebeans mapped class for the program object. */
@Entity
@Table(name = "programs")
/** The ebeans mapped class for the program object. */
public class Program extends BaseModel {

  private ProgramDefinition programDefinition;

  private @Constraints.Required String name;

  private @Constraints.Required String description;

  private @Constraints.Required @DbJson BlockContainer blockDefinitions;

  public ProgramDefinition getProgramDefinition() {
    return this.programDefinition;
  }

  public Program(ProgramDefinition definition) {
    this.programDefinition = definition;
  }

  /** Populates column values from {@link ProgramDefinition} */
  @PrePersist
  public void persistChangesToProgramDefinition() throws JsonProcessingException {
    this.id = this.programDefinition.id();
    this.name = this.programDefinition.name();
    this.description = this.programDefinition.description();
    this.blockDefinitions = BlockContainer.create(this.programDefinition.blockDefinitions());
  }

  /** Populates {@link ProgramDefinition} from column values. */
  @PostLoad
  public void loadProgramDefinition() throws JsonProcessingException {
    this.programDefinition =
        ProgramDefinition.builder()
            .setId(this.id)
            .setName(this.name)
            .setDescription(this.description)
            .setBlockDefinitions(this.blockDefinitions.blockDefinitions())
            .build();
  }
}
