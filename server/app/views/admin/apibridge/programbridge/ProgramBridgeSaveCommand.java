package views.admin.apibridge.programbridge;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import play.data.validation.Constraints;

/**
 * Holds for data POSTed when saving the program bridge bindings. This is a class because Play
 * doesn't currently support data binding records
 */
@Data
public final class ProgramBridgeSaveCommand {
  @Constraints.Required private String bridgeAdminName;
  private List<ProgramBridgeSaveField> inputFields = new ArrayList<>();
  private List<ProgramBridgeSaveField> outputFields = new ArrayList<>();
}
