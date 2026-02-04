package views.admin.apibridge.programbridge;

import lombok.Data;
import play.data.validation.Constraints;

/**
 * Holds for data POSTed when selecting the bridge to edit. This is a class because Play doesn't
 * currently support data binding records
 */
@Data
public final class ProgramBridgeEditCommand {
  @Constraints.Required private String bridgeAdminName;
}
