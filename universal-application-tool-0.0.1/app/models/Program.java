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

/** The ebeans mapped class for the program object. */
@Entity
@Table(name = "programs")
public class Program extends BaseModel {

  private static ObjectMapper mapper =
      new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

  private ProgramDefinition programDefinition;

  private @Constraints.Required String name;

  private @Constraints.Required String description;

  private @Constraints.Required @DbJson String blockDefinitions;

  public ProgramDefinition getProgramDefinition() {
    return this.programDefinition;
  }

  public Program(ProgramDefinition definition) {
    this.programDefinition = definition;
  }

  /** Populates column values from {@link programDefinition} */
  @PrePersist
  public void serializeBlockDefinitions() throws JsonProcessingException {
    this.id = this.programDefinition.id();
    this.name = this.programDefinition.name();
    this.description = this.programDefinition.description();
    this.blockDefinitions = mapper.writeValueAsString(this.programDefinition.blockDefinitions());
  }

  /** Populates {@link programDefinition} from column values. */
  @PostLoad
  public void loadProgramDefinition() throws JsonProcessingException {
    this.programDefinition =
        ProgramDefinition.builder()
            .setId(this.id)
            .setName(this.name)
            .setDescription(this.description)
            .setBlockDefinitions(
                mapper.readValue(
                    this.blockDefinitions, new TypeReference<ImmutableList<BlockDefinition>>() {}))
            .build();
  }
}
