package models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.DbJsonB;
import io.ebean.text.json.EJson;
import java.io.IOException;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import play.data.validation.Constraints;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import services.program.ProgramDefinition;

@Entity
@Table(name = "programs")
/** The ebeans mapped class for the program object. */
public class Program extends BaseModel {

  public ProgramDefinition programDefinition;

  @Constraints.Required public String name;

  @Constraints.Required public String description;

  @Constraints.Required @DbJson private String block_definitions;

  public void setProgramDefinition(ProgramDefinition definition) {
    this.programDefinition = definition;
  }

  @PrePersist
  public void serializeBlockDefinitions() throws JsonProcessingException {
    this.name = this.programDefinition.name();
    this.description = this.programDefinition.description();
    ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
    this.block_definitions = mapper.writeValueAsString(this.programDefinition.blockDefinitions());
  }
}
