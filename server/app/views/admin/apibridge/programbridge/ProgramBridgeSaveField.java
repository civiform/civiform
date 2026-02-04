package views.admin.apibridge.programbridge;

import lombok.Data;

/**
 * Holds single item used with {@link ProgramBridgeSaveCommand} for data POSTed when saving the
 * program bridge bindings. This is a class because Play doesn't currently support databinding
 * records
 */
@Data
public final class ProgramBridgeSaveField {
  private String questionName;
  private String questionScalar;
  private String externalName;
}
