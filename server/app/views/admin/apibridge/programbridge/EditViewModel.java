package views.admin.apibridge.programbridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import services.program.ProgramDefinition;

public record EditViewModel(ProgramDefinition programDefinition) {

  public String getBridgeDefinitions() {
    ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
    try {
      return mapper.writeValueAsString(programDefinition().bridgeDefinitions());
    } catch (JsonProcessingException e) {
      return "bad parse bridge defs";
    }
  }
}
