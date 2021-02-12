package models;

import io.ebean.annotation.DbJsonB;
import javax.persistence.Entity;
import javax.persistence.Table;
import play.data.validation.Constraints;

@Entity
@Table(name = "programs")
/** The ebeans mapped class for the program object. */
public class Program extends BaseModel {

  @Constraints.Required public String name;

  @Constraints.Required public String description;

  @Constraints.Required @DbJsonB public BlockContainer blockContainer;

  //  private ProgramDefinition programDefinition;

  //  public ProgramDefinition getProgramDefinition() {
  //    if (this.programDefinition == null) {
  //      this.programDefinition =
  //          ProgramDefinition.builder()
  //              .setId(this.serialVersionUID)
  //              .setName(this.name)
  //              .setDescription(this.description)
  //              .setBlockDefinitions(this.blockContainer.blockDefinitions())
  //              .build();
  //    }
  //    return this.programDefinition;
  //  }
}
