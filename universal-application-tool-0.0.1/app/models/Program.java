package models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import io.ebean.annotation.DbJson;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;

@Entity
@Table(name = "programs")
/** The ebeans mapped class for the program object. */
public class Program extends BaseModel {

  private static ObjectMapper mapper =
      new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

  private ProgramDefinition programDefinition;

  @Constraints.Required String name;

  @Constraints.Required String description;

  @Constraints.Required @DbJson String block_definitions;

  public ProgramDefinition getProgramDefinition() {
    return this.programDefinition;
  }

  public void setProgramDefinition(ProgramDefinition definition) {
    this.programDefinition = definition;
  }

  @PrePersist
  /** Populates column values from {@link programDefinition} */
  public void serializeBlockDefinitions() throws JsonProcessingException {
    this.id = this.programDefinition.id();
    this.name = this.programDefinition.name();
    this.description = this.programDefinition.description();
    this.block_definitions = mapper.writeValueAsString(this.programDefinition.blockDefinitions());
  }

  @PostLoad
  /** Populates {@link programDefinition} from column values. */
  public void loadProgramDefinition() throws JsonProcessingException {
    this.programDefinition =
        ProgramDefinition.builder()
            .setId(this.id)
            .setName(this.name)
            .setDescription(this.description)
            .setBlockDefinitions(
                mapper.readValue(
                    this.block_definitions, new TypeReference<ImmutableList<BlockDefinition>>() {}))
            .build();
  }
}
